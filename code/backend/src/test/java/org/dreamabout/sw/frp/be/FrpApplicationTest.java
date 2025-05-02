package org.dreamabout.sw.frp.be;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class FrpApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadText() {
        assert(applicationContext != null);
    }

}
