import aed.tables.tests.CuckooHashTableTests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

public class MooshakMain {

    public static void main(String[] args)
    {
        HashMap<String, List<Runnable>> unitTests = new HashMap<>();

        //problem A - Unit tests for problem A
        unitTests.put("A", CuckooHashTableTests.getAllTests());
        //problem B - Unit tests for problem B


        InputStreamReader inputReader = new InputStreamReader(System.in);
        BufferedReader bReader = new BufferedReader(inputReader);

        try
        {
            String line = bReader.readLine();
            String[] lineArgs = line.split(" ");
            String problem = lineArgs[0];
            int testNumber = Integer.parseInt(lineArgs[1])-1;

            unitTests.get(problem).get(testNumber).run();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
