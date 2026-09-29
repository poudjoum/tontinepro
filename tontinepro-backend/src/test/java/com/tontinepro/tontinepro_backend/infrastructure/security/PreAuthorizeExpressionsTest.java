package com.tontinepro.tontinepro_backend.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les expressions {@code @PreAuthorize} ne sont évaluées qu'à l'appel : une faute
 * de frappe (paramètre inexistant, méthode de {@code @sec} mal nommée) ne se
 * verrait qu'en production, sous forme d'erreur 500 ou de refus inexpliqué.
 * Ce test les vérifie toutes sans démarrer l'application.
 */
class PreAuthorizeExpressionsTest {

    private static final Map<String, Class<?>> BEANS = Map.of(
            "sec", SecurityExpressionService.class,
            "tontineDe", TontineDeResolver.class);

    private static final Pattern APPEL_BEAN = Pattern.compile("@(\\w+)\\.(\\w+)\\(");
    private static final Pattern VARIABLE = Pattern.compile("#(\\w+)");

    @Test
    void toutesLesExpressionsSontValides() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        var parser = new SpelExpressionParser();
        List<String> erreurs = new ArrayList<>();
        int nb = 0;

        for (BeanDefinition bd : scanner.findCandidateComponents("com.tontinepro.tontinepro_backend.api")) {
            Class<?> controller = Class.forName(bd.getBeanClassName());
            PreAuthorize surClasse = controller.getAnnotation(PreAuthorize.class);

            for (Method m : controller.getDeclaredMethods()) {
                PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
                if (pa == null) pa = surClasse;
                if (pa == null || !java.lang.reflect.Modifier.isPublic(m.getModifiers())) continue;
                String lieu = controller.getSimpleName() + "." + m.getName() + " : ";
                String expr = pa.value();
                nb++;

                try {
                    parser.parseExpression(expr);
                } catch (Exception e) {
                    erreurs.add(lieu + "syntaxe invalide — " + e.getMessage());
                    continue;
                }

                Set<String> params = Arrays.stream(m.getParameters())
                        .map(Parameter::getName).collect(Collectors.toSet());
                Matcher v = VARIABLE.matcher(expr);
                while (v.find()) {
                    if (!params.contains(v.group(1))) {
                        erreurs.add(lieu + "paramètre #" + v.group(1) + " absent de la méthode");
                    }
                }

                Matcher b = APPEL_BEAN.matcher(expr);
                while (b.find()) {
                    Class<?> bean = BEANS.get(b.group(1));
                    String methode = b.group(2);
                    if (bean == null) {
                        erreurs.add(lieu + "bean @" + b.group(1) + " inconnu");
                    } else if (Arrays.stream(bean.getMethods()).noneMatch(x -> x.getName().equals(methode))) {
                        erreurs.add(lieu + "méthode @" + b.group(1) + "." + methode + " inexistante");
                    }
                }
            }
        }

        assertThat(nb).as("expressions trouvées").isGreaterThan(50);
        assertThat(erreurs).isEmpty();
    }
}
