package io.vertx.tests;

import io.vertx.docgen.processor.impl.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Checks the behavior of the entity un-escaper.
 */
public class EntityUtilsTest {

  @Test
  public void testUnescapeEntity() throws Exception {
    Assert.assertEquals(EntityUtils.unescapeEntity("#92"), "\\");
    Assert.assertEquals(EntityUtils.unescapeEntity("u00A7"), "§");
    Assert.assertEquals(EntityUtils.unescapeEntity("#x020AC"), "€");
    Assert.assertEquals(EntityUtils.unescapeEntity("nbsp"), "&nbsp;");
    Assert.assertEquals(EntityUtils.unescapeEntity(""), "");
    Assert.assertEquals(EntityUtils.unescapeEntity(null), "");
    Assert.assertEquals(EntityUtils.unescapeEntity("\t"), "");

    Assert.assertEquals(EntityUtils.unescapeEntity("#t"), "#t");
    Assert.assertEquals(EntityUtils.unescapeEntity("uzz02"), "uzz02");
    Assert.assertEquals(EntityUtils.unescapeEntity("#"), "#");
    Assert.assertEquals(EntityUtils.unescapeEntity("#x"), "#x");
  }
}
