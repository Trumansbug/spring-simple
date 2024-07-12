package cn.clear.controller;


import cn.clear.springframework.annotation.Controller;
import cn.clear.springframework.annotation.RequestMapping;
import cn.clear.springframework.annotation.RequestParam;
import cn.clear.springframework.webmvc.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

@Controller
public class HelloController {

    @RequestMapping("/hello")
    public ModelAndView hello() {
        HashMap<String, Object> model = new HashMap<>();
        model.put("data1", "hello");
        model.put("data2", "world");
        return new ModelAndView("test", model);
    }


    @RequestMapping("/hello1")
    public ModelAndView hello1(@RequestParam("name") String name, HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> model = new HashMap<>();
        model.put("data1", "hello");
        model.put("data2", "world");
        return new ModelAndView("test", model);
    }
}
