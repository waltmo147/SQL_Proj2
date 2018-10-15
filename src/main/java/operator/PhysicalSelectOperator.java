package operator;

import logical.operator.SelectOperator;
import model.Tuple;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.PlainSelect;
import util.SelectExpressionVisitor;
import util.JoinExpressionVisitor;

import java.util.Deque;
import java.util.Map;

public class PhysicalSelectOperator extends PhysicalOperator {

    private PhysicalOperator prevOp;
    private Expression expression;
    private Map<String, Integer> currentSchema;

    /**
     * Constructor of PhysicalSelectOperator
     * @param operator previous (child) operator
     * @param plainSelect plain sql sentence
     */
    public PhysicalSelectOperator(PhysicalOperator operator, PlainSelect plainSelect) {
        this.prevOp = operator;
        this.currentSchema = operator.getSchema();
        this.expression = plainSelect.getWhere();
        
        JoinExpressionVisitor joinExpress = new JoinExpressionVisitor(this.currentSchema);
        expression.accept(joinExpress);
        expression = joinExpress.getExpression();
    }

    public PhysicalSelectOperator(SelectOperator logSelectOp, Deque<PhysicalOperator> physOpChildren) {
        this.prevOp = physOpChildren.pop();
        this.expression = logSelectOp.getExpression();
        this.currentSchema = logSelectOp.getSchema();

        JoinExpressionVisitor joinExpress = new JoinExpressionVisitor(this.currentSchema);
        expression.accept(joinExpress);
        expression = joinExpress.getExpression();
    }

    /**
     * @return the next tuple filtered by the Select PhysicalOperator
     */
    @Override
    public Tuple getNextTuple() {
        Tuple next = prevOp.getNextTuple();
        if (expression != null) {
            while (next != null) {
                SelectExpressionVisitor sv = new SelectExpressionVisitor(next, prevOp.getSchema());
                expression.accept(sv);
                if (sv.getResult()) {
                    break;
                }
                next = prevOp.getNextTuple();
            }
        }
        return next;
    }

    /**
     * reset the select operator would be resetting the previous operator
     */
    @Override
    public void reset() {
        prevOp.reset();
    }

    /**
     * @return the schema of select operator which is the same with the previous schema
     */
    @Override
    public Map<String, Integer> getSchema() {
        return currentSchema;
    }
}
