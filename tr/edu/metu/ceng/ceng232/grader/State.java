
package tr.edu.metu.ceng.ceng232.grader;

import com.cburch.logisim.data.Value;
import java.util.List;
import java.util.Map;
import java.util.regex.*;

/**
 *
 * @author kerem
 */
public class State {
    public static enum TYPE {
        TRUTH_TABLE,
        CONDITION,
        REGISTER_MODIFY,
    };

    protected char[] inputs;
    protected char[] outputs;
    protected char[] stateFrom;
    protected char[] stateTo;
    protected int gotoState;
    protected int gotoLimit;
    protected TYPE type;

    protected State(String loadFrom) throws Exception {
        // either "1,0,0,0,0;X,X,X" or "if X,1,X then goto 2 at most 1000 times"
        // or "modify 74x95 state from 0,0,1,1 to 1,1,0,1" (X is not allowed here, only 0 or 1)
        //
        String str = loadFrom.trim();
        Pattern p1 = Pattern.compile("^(([01],)*[01]);(([01X],)*[01X])$");
        Pattern p2 = Pattern.compile("^if +(([01X],)*[01X]) +then +goto +([0-9]+) +at +most +([0-9]+) +times$");
        Pattern p3 = Pattern.compile("^modify +74x95 +state +from +([01],[01],[01],[01]) +to +([01],[01],[01],[01])$");
        Matcher m1 = p1.matcher(str);
        Matcher m2 = p2.matcher(str);
        Matcher m3 = p3.matcher(str);

        if (m1.matches()) {
            String[] sInputs = m1.group(1).split(",");
            String[] sOutputs = m1.group(3).split(",");

            inputs = new char[sInputs.length];
            for (int i = 0; i < sInputs.length; i++)
                inputs[i] = sInputs[i].charAt(0);

            outputs = new char[sOutputs.length];
            for (int i = 0; i < sOutputs.length; i++)
                outputs[i] = sOutputs[i].charAt(0);

            type = TYPE.TRUTH_TABLE;
        }
        else if (m2.matches()) {
            String[] sOutputs = m2.group(1).split(",");
            String sNewState = m2.group(3);
            String sAtMost = m2.group(4);

            outputs = new char[sOutputs.length];
            for (int i = 0; i < sOutputs.length; i++)
                outputs[i] = sOutputs[i].charAt(0);

            try {
                gotoState = Integer.parseInt(sNewState);
                gotoLimit = Integer.parseInt(sAtMost);
            }
            catch (NumberFormatException e) {
            }

            type = TYPE.CONDITION;
        }
        else if (m3.matches()) {
            String[] sFrom = m3.group(1).split(",");
            String[] sTo = m3.group(2).split(",");

            stateFrom = new char[sFrom.length];
            for (int i = 0; i < sFrom.length; i++)
                stateFrom[i] = sFrom[i].charAt(0);

            stateTo = new char[sTo.length];
            for (int i = 0; i < sTo.length; i++)
                stateTo[i] = sTo[i].charAt(0);

            type = TYPE.REGISTER_MODIFY;
        }
        else
            throw new Exception("Invalid state string: " + str);
    }
}
