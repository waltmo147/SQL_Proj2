package logical.operator;

import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.Test;
import java.util.Map;
import java.util.HashMap;

import junit.framework.Assert;

import static org.junit.Assert.*;
import java.io.StringReader;

public class SortOperatorTest {
    SortOperator sortOp;

    public SortOperatorTest() throws Exception{
        String statement = "SELECT * FROM Boats AS BT ORDER BY BT.F;";
        CCJSqlParserManager parserManager = new CCJSqlParserManager();
        PlainSelect plainSelect = (PlainSelect) ((Select) parserManager.parse(new StringReader(statement))).getSelectBody();
        Operator op = new ScanOperator(plainSelect, 0);

        sortOp = new SortOperator(op, plainSelect);
    }
    
    @Test
    public void getOrder()  {
        Assert.assertEquals("Order: ", "[BT.F]", sortOp.getOrder().toString());
    }

    @Test
    public void getChildrenTest()  {
        Assert.assertEquals("Children: ", 1, sortOp.getChildren().length);
    }

    @Test
    public void getSchema() {
        Map<String, Integer> expectedSchema = new HashMap<>();
        expectedSchema.put("BT.D", 0);
        expectedSchema.put("BT.E", 1);
        expectedSchema.put("BT.F", 2);
        Assert.assertEquals(expectedSchema, sortOp.getSchema());

    }
}