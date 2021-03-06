//Compiler Test
//=============

package org.mellowd;

import org.mellowd.io.Compiler;
import org.mellowd.io.CompilerOptions;
import org.mellowd.compiler.CompilationException;
import org.mellowd.compiler.ParseException;
import org.mellowd.compiler.SyntaxErrorReport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class CompilerTest {

    //A class used to teach Gson how to parse the input data. Gson used a reflection
    //based deserialization strategy to translate from JSON a json description to
    //a java object. Each field is the name of the JSON object parameter.
    private static final class CompilerTestData {
        public String compFileName;
        public byte timeNumerator;
        public byte timeDenominator;
        public int tempo;
    }

    private static final File OUTDIR = new File("compTestOut");
    private static final AtomicInteger TEST_NUM = new AtomicInteger(0);

    //Test Data Loader
    //----------------
    //Read the test data input configuration and build a test instance for each
    //case described in the file. Each `Object[]` is a set of constructor arguments
    //that will be used to create a new `CompilerTest` instance who's [compileTest()](#test-case)
    //will be invoked.
    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> loadTests() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("testdata/CompilerTestData.json")));
        Gson gson = new GsonBuilder().create();
        CompilerTestData[] data = gson.fromJson(reader, CompilerTestData[].class);
        reader.close();
        List<Object[]> constructorArgs = new ArrayList<>(data.length);
        for (CompilerTestData testData : data) {
            constructorArgs.add(new Object[]{
                    testData.compFileName,
                    testData.timeNumerator,
                    testData.timeDenominator,
                    testData.tempo
            });
        }
        return constructorArgs;
    }

    //Each test will have a file to compile and the time signature and tempo to compile it
    //with. The `outFile` will be a file of the same name inside the `compTestOut` directory.
    private File outFile;
    private File toCompile;
    private final byte timeNumerator;
    private final byte timeDenominator;
    private final int tempo;

    public CompilerTest(String toCompName, byte timeNumerator, byte timeDenominator, int tempo) throws URISyntaxException {
        this.timeNumerator = timeNumerator;
        this.timeDenominator = timeDenominator;
        this.tempo = tempo;
        String toCompPath = Thread.currentThread().getContextClassLoader().getResource("compilertest" + File.separator + toCompName).toURI().getPath();
        this.toCompile = new File(toCompPath);
        this.outFile = new File(toCompile.getParentFile(), OUTDIR + File.separator + toCompile.getName().replace(Compiler.FILE_EXTENSION, ".mid"));
        if (!outFile.getParentFile().exists())
            outFile.getParentFile().mkdirs();
    }

    //Test Case
    //---------
    @Test
    public void compileTest() {
        try {
            //Print out some information about the test. This includes a marker ID which is simply
            //a number that uniquely identifies a test from this set. It also displays some test
            //parameters.
            System.out.printf("Compiler Test %d: InFile=%s  OutFile=%s  Tempo=%d TimSig=%d/%d\n",
                    TEST_NUM.getAndIncrement(), this.toCompile.getName(), this.outFile.getName(),
                    this.tempo, this.timeNumerator, this.timeDenominator);
            System.out.print("-----------------------------------------------------------------------------------\n");
            CompilerOptions options = new CompilerOptions.Builder()
                    .setTimeSignature(timeNumerator, timeDenominator)
                    .setTempo(tempo)
                    .setSilent(true)
                    .build();
            Sequence compilationResult = Compiler.compile(toCompile, options);
            writeOut(compilationResult);
            System.out.println();
        } catch (IOException e) {
            fail(String.format("Error reading input file (%s). Reason: %s\n",
                    toCompile.getAbsolutePath(), e.getLocalizedMessage()));
        } catch (CompilationException e) {
            Throwable cause = e.getCause();
            String message = String.format("Compilation exception on line %d@%d-%d:'%s'. Problem: %s\n",
                    e.getLine(),
                    e.getStartPosInLine(),
                    e.getStartPosInLine() + (e.getStop() - e.getStart()),
                    e.getText(),
                    cause.getMessage());
            System.out.println(message);
            fail(message);
        } catch (ParseException e) {
            for (SyntaxErrorReport errorReport : e.getProblems()) {
                System.out.println(errorReport.getErrorType().toString()+": "+errorReport.getMessage());
            }
            fail("Exception while parsing.");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail("Exception thrown while executing. "+e.getMessage());
        }
    }

    private void writeOut(Sequence compilationResult) throws IOException {
        //Create the output file if it doesn't exist.
        if (!outFile.exists()) {
            outFile.getParentFile().mkdirs();
            outFile.createNewFile();
        }

        //If the compilation result is empty then append the EOT_MESSAGE to make the track playable
        if (compilationResult.getTickLength() == 0) {
            compilationResult.getTracks()[0].add(new MidiEvent(Compiler.EOT_MESSAGE, 1));
        }

        //Write the result to the out file. The type 1 midi format is the standard
        //type for multi track sequences which we have in our case.
        OutputStream outStream = new FileOutputStream(outFile);
        MidiSystem.write(compilationResult, 1, outStream);
        outStream.close();
        System.out.println("Compilation successful.");
    }
}
