package cn.clear.springframework.webmvc.servlet;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class View {

    /**模板*/
    private final File viewFile;

    /**占位符表达式*/
    private final Pattern pattern = Pattern.compile("#\\{[^\\}]+\\}", Pattern.CASE_INSENSITIVE);

    public View(File viewFile) {
        this.viewFile = viewFile;
    }

    /**
     * 渲染
     */
    public void render(Map<String, ?> model,
                       HttpServletRequest request,
                       HttpServletResponse response) throws Exception {

        StringBuilder sb = new StringBuilder();
        try (RandomAccessFile ra = new RandomAccessFile(this.viewFile, "r");) {
            String line;

            while (null != (line = ra.readLine())) {
                line = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                Matcher matcher = this.pattern.matcher(line);
                // 找到下一个占位符
                while (matcher.find()) {
                    String paramName = matcher.group();
                    paramName = paramName.replaceAll("#\\{|\\}", "");
                    Object paramValue = model.get(paramName);
                    if (null == paramValue) {
                        continue;
                    }
                    // 替换占位符为实际值
                    line = matcher.replaceFirst(makeStringForRegExp(paramValue.toString()));
                    // 接着匹配下一个占位符
                    matcher = pattern.matcher(line);
                }
                sb.append(line);
            }
        }

        response.setCharacterEncoding("utf-8");
        // 输出到response
        response.getWriter().write(sb.toString());
    }


    /**
     * 处理特殊字符
     */
    public static String makeStringForRegExp(String str) {
        return str.replace("\\", "\\\\").replace("*", "\\*")
                .replace("+", "\\+").replace("|", "\\|")
                .replace("{", "\\{").replace("}", "\\}")
                .replace("(", "\\(").replace(")", "\\)")
                .replace("^", "\\^").replace("$", "\\$")
                .replace("[", "\\[").replace("]", "\\]")
                .replace("?", "\\?").replace(",", "\\,")
                .replace(".", "\\.").replace("&", "\\&");
    }
}
