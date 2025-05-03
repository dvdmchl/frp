package org.dreamabout.sw.frp.be;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class EntitiesConventionTest {

    private static final String BASE_PACKAGE = "org.dreamabout.sw.frp.be";

    @Test
    void equalsAndHashCodeShouldUseOnlyId() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        PodamFactory podamFactory = new PodamFactoryImpl();

        // scan your entities exactly as before…
        for (BeanDefinition bd : scanner.findCandidateComponents(BASE_PACKAGE)) {
            Class<?> entityClass = Class.forName(bd.getBeanClassName());
            // create two instances
            Object a = podamFactory.manufacturePojo(entityClass);
            Object b = podamFactory.manufacturePojo(entityClass);

            // set id = 1 on both
            setField(a, "id", 1L);
            setField(b, "id", 1L);


            // they must still be equal because only `id` is used
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).hasSameHashCodeAs(b);

            // now change b.id → not equal
            setField(b, "id", 2L);
            assertThat(a).isNotEqualTo(b);
        }
    }

    // helper to set fields via reflection
    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
