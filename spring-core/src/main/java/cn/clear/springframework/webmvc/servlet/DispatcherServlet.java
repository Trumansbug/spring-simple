package cn.clear.springframework.webmvc.servlet;



import cn.clear.springframework.annotation.Controller;
import cn.clear.springframework.annotation.RequestMapping;
import cn.clear.springframework.context.support.DefaultApplicationContext;


import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DispatcherServlet extends HttpServlet {

    /**配置文件地址，从web.xml中获取*/
    private static final String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";

    private DefaultApplicationContext context;

    private final List<HandlerMapping> handlerMappings = new ArrayList<>();

    private final Map<HandlerMapping, HandlerAdapter> handlerAdapters = new HashMap<>();

    private final List<ViewResolver> viewResolvers = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            this.doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception,Details:\r\n"
                    + Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\]", "")
                    .replaceAll(",\\s", "\r\n"));
            e.printStackTrace();
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // 1、通过从request中拿到URL，去匹配一个HandlerMapping
        HandlerMapping handler = getHandler(req);

        if (handler == null) {
            // 没有找到handler返回404
            processDispatchResult(req, resp, new ModelAndView("404"));
            return;
        }

        // 2、准备调用前的参数
        HandlerAdapter ha = getHandlerAdapter(handler);

        // 3、真正的调用controller的方法
        ModelAndView mv = ha.handle(req, resp, handler);

        // 4、渲染页面输出
        processDispatchResult(req, resp, mv);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1、初始化ApplicationContext容器
        context = new DefaultApplicationContext(config.getInitParameter(CONTEXT_CONFIG_LOCATION));

        // 2、初始化Spring MVC 九大组件
        initStrategies(context);
    }


    /**
     * 初始化策略
     * @param context ApplicationContext容器
     */
    protected void initStrategies(DefaultApplicationContext context) {
        // TODO 多文件上传的组件

        // TODO 初始化本地语言环境

        // TODO 初始化模板处理器

        // handlerMapping
        initHandlerMappings(context);

        // 初始化参数适配器
        initHandlerAdapters(context);

        // TODO 初始化异常拦截器

        // TODO 初始化视图预处理器

        // 初始化视图转换器
        initViewResolvers(context);

        // TODO 参数缓存器
    }

    /**
     * 初始化HandlerMapping，提取用户设置的Controller、浏览器能访问到的方法，以及使用@RequestMapping定义的URL表达式
     * @param context ApplicationContext容器
     */
    private void initHandlerMappings(DefaultApplicationContext context) {
        // 获取容器中注册的bean名称数组
        String[] beanDefinitionNames = context.getBeanDefinitionNames();
        try {
            for (String beanDefinitionName : beanDefinitionNames) {
                Object bean = context.getBean(beanDefinitionName);
                // 获取bean的类信息
                Class<?> clazz = bean.getClass();
                // 如果bean没有@Controller注解，跳过
                if (!clazz.isAnnotationPresent(Controller.class)) {
                    continue;
                }
                
                // 拿到类上定义的baseUrl
                String baseUrl = "";
                if (clazz.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                    baseUrl = requestMapping.value();
                }

                // 遍历该类上的方法
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    // 处理@RequstMapping注解的方法
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        
                        // 映射URL
                        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                        String regex = ("/" + baseUrl + "/" + requestMapping.value().replaceAll("\\*", ".*")).replaceAll("/+", "/");
                        Pattern pattern = Pattern.compile(regex);
                        
                        handlerMappings.add(new HandlerMapping(bean, method, pattern));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void initHandlerAdapters(DefaultApplicationContext context) {
        for (HandlerMapping handlerMapping : handlerMappings) {
            handlerAdapters.put(handlerMapping, new HandlerAdapter());
        }
    }

    private void initViewResolvers(DefaultApplicationContext context) {
        // 配置文件中拿到模板的存放目录
        String templateRoot = context.getConfig().getProperty("spring.template.root");
        URL url = this.getClass().getClassLoader().getResource(templateRoot);
        if (url != null) {
            this.viewResolvers.add(new ViewResolver(templateRoot));
        }
    }

    private HandlerMapping getHandler(HttpServletRequest req) throws Exception {
        if (this.handlerMappings.isEmpty()) {
            return null;
        }

        // 请求Url
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        // 匹配url，找到对应的方法
        for (HandlerMapping handler : this.handlerMappings) {
            Matcher matcher = handler.getPattern().matcher(url);
            // 如果没有匹配上继续下一个匹配
            if (!matcher.matches()) {
                continue;
            }
            return handler;
        }
        return null;
    }

    private HandlerAdapter getHandlerAdapter(HandlerMapping handler) throws ServletException {
        if (this.handlerAdapters.isEmpty() || !this.handlerAdapters.containsKey(handler)) {
            throw new ServletException("No adapter for handler [" + handler + "]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
        }
        
        return this.handlerAdapters.get(handler);
    }

    private void processDispatchResult(HttpServletRequest req, HttpServletResponse resp, ModelAndView mv) throws Exception {
        if (null == mv) {
            return;
        }

        if (this.viewResolvers.isEmpty()) {
            return;
        }

        for (ViewResolver viewResolver : this.viewResolvers) {
            //根据模板名拿到View
            View view = viewResolver.resolveViewName(mv.getViewName(), null);
            //开始渲染
            view.render(mv.getModel(), req, resp);
            return;
        }
    }


}    
