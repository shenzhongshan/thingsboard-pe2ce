/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.codec.CharEncoding
 *  org.thymeleaf.TemplateEngine
 *  org.thymeleaf.context.Context
 *  org.thymeleaf.context.IContext
 *  org.thymeleaf.templatemode.TemplateMode
 *  org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
 *  org.thymeleaf.templateresolver.ITemplateResolver
 *  org.thymeleaf.templateresolver.StringTemplateResolver
 */
package org.thingsboard.server.report.util;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.CharEncoding;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

public class ThymeleafUtil {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern IMAGE_SRC_PATTERN = Pattern.compile("<img[^>]+src=[\"']\\$\\{([^\"'}]+)}[\"']", 2);
    private static final TemplateEngine htmlClassEngine = new TemplateEngine();
    private static final TemplateEngine htmlStringEngine;
    private static final TemplateEngine textStringEngine;
    private static final TemplateEngine svgClassEngine;

    public static String renderFromHtmlTemplate(String templateHtml, Map<String, Object> variables) {
        Context context = new Context(Locale.getDefault(), variables);
        return htmlClassEngine.process(templateHtml, (IContext)context);
    }

    public static String renderFromTextString(String html, Map<String, Object> variables) {
        ThymeleafUtil.populateMissingImagePlaceholders(html, variables);
        Context context = new Context();
        context.setVariables(variables);
        return textStringEngine.process(ThymeleafUtil.convertToThymeleafInline(ThymeleafUtil.sanitize(html)), (IContext)context);
    }

    private static void populateMissingImagePlaceholders(String html, Map<String, Object> variables) {
        Matcher matcher = IMAGE_SRC_PATTERN.matcher(html);
        while (matcher.find()) {
            String str;
            String key = matcher.group(1);
            Object value = variables.get(key);
            if (value != null && (!(value instanceof String) || !(str = (String)value).isEmpty())) continue;
            variables.put(key, "noImage");
        }
    }

    public static String renderFromSvgTemplate(String templateSvg, Map<String, Object> variables) {
        Context context = new Context(Locale.getDefault(), variables);
        return svgClassEngine.process(templateSvg, (IContext)context);
    }

    public static String convertToThymeleafInline(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String var;
            matcher.appendReplacement(result, Matcher.quoteReplacement((String)(switch (var = matcher.group(1).trim()) {
                case "pageNumber" -> "<span class=\"page-number\"></span>";
                case "totalPages" -> "<span class=\"page-count\"></span>";
                default -> {
                    String safeKey = ThymeleafUtil.normalizeVariableName(var);
                    yield "[(${" + safeKey + "})]";
                }
            })));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static String normalizeVariableName(String var) {
        return var.trim().replaceAll("\\s+", "_");
    }

    private static String sanitize(String html) {
        return html.replace("\n", "");
    }

    static {
        ClassLoaderTemplateResolver classResolver = new ClassLoaderTemplateResolver();
        classResolver.setPrefix("/");
        classResolver.setSuffix(".html");
        classResolver.setTemplateMode(TemplateMode.HTML);
        classResolver.setOrder(Integer.valueOf(1));
        classResolver.setCharacterEncoding(CharEncoding.UTF_8);
        htmlClassEngine.setTemplateResolver((ITemplateResolver)classResolver);
        htmlStringEngine = new TemplateEngine();
        StringTemplateResolver stringResolver = new StringTemplateResolver();
        stringResolver.setTemplateMode(TemplateMode.HTML);
        stringResolver.setResolvablePatterns(Set.of("*"));
        stringResolver.setCacheable(false);
        stringResolver.setOrder(Integer.valueOf(2));
        htmlStringEngine.setTemplateResolver((ITemplateResolver)stringResolver);
        textStringEngine = new TemplateEngine();
        StringTemplateResolver textStringResolver = new StringTemplateResolver();
        textStringResolver.setTemplateMode(TemplateMode.TEXT);
        textStringResolver.setResolvablePatterns(Set.of("*"));
        textStringResolver.setCacheable(false);
        textStringResolver.setOrder(Integer.valueOf(3));
        textStringEngine.setTemplateResolver((ITemplateResolver)textStringResolver);
        svgClassEngine = new TemplateEngine();
        ClassLoaderTemplateResolver svgClassResolver = new ClassLoaderTemplateResolver();
        svgClassResolver.setPrefix("/");
        svgClassResolver.setSuffix(".svg");
        svgClassResolver.setTemplateMode(TemplateMode.XML);
        svgClassResolver.setOrder(Integer.valueOf(4));
        svgClassResolver.setCharacterEncoding(CharEncoding.UTF_8);
        svgClassEngine.setTemplateResolver((ITemplateResolver)svgClassResolver);
    }
}

