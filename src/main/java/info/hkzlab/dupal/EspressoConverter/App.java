package info.hkzlab.dupal.EspressoConverter;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.hkzlab.dupal.EspressoConverter.devices.PALSpecs;
import info.hkzlab.dupal.EspressoConverter.formatter.EspressoFormatter;
import info.hkzlab.dupal.EspressoConverter.parser.ContentParser;
import info.hkzlab.dupal.EspressoConverter.states.*;

/**
 * Hello world!
 *
 */
public class App {
    private final static Logger logger = LoggerFactory.getLogger(App.class);
    private final static String version = App.class.getPackage().getImplementationVersion();

    private static String inFile = null;
    private static String outFile = null;

    public static void main(String[] args) throws IOException {
        System.out.println("DuPAL Espresso Converter " + version);

        if (args.length < 2) {
            logger.error("Wrong number of arguments passed.\n"
                    + "dupal_espressoconverter <input_file> <output_file>\n");

            return;
        }

        parseArgs(args);

        JSONObject root = new JSONObject(new JSONTokener(new FileReader(inFile)));
        PALSpecs pSpecs = ContentParser.getPALType(root);
        logger.info("Got file for PAL type " + pSpecs);

        String header = null;
        String[] table = null;
        String footer = EspressoFormatter.formatEspressoFooter();

        if(pSpecs.getPinCount_IO() > 0 || pSpecs.getPinCount_RO() > 0) {
            int IOsAsOUTs = ContentParser.extractIOasOutMask(root);
            RLink[] rlArray = ContentParser.extractRLinks(root);
            OLink[] olArray = ContentParser.extractOLinks(root);
            
            header = EspressoFormatter.formatEspressoTableHeader(pSpecs, IOsAsOUTs, 15);
            table = EspressoFormatter.formatEspressoTable(pSpecs, IOsAsOUTs, olArray, rlArray, 15);

        } else {
            SimpleState[] ssArray = ContentParser.extractSimpleStates(root);
            header = EspressoFormatter.formatEspressoTableHeader(pSpecs, 0, -1);
            table = EspressoFormatter.formatEspressoTable(pSpecs, ssArray);
        }

        saveTableToFile(outFile, header, table, footer);
    }

    private static void parseArgs(String[] args) {
        inFile = args[0];
        outFile = args[1];
    }

    private static void saveTableToFile(String destination, String header, String[] rows, String footer) throws IOException {
        FileOutputStream fout = null;
        
        logger.info("saveOutputToFile() -> Saving to " + destination);

        try {
            fout = new FileOutputStream(outFile);

            fout.write(header.getBytes(StandardCharsets.US_ASCII));
            for(String row : rows) fout.write(row.getBytes(StandardCharsets.US_ASCII));
            fout.write(footer.getBytes(StandardCharsets.US_ASCII));
            
            fout.flush();
            fout.close();
        } catch(IOException e) {
            logger.error("Error printing out the registered outputs table (not including outputs).");
            throw e;
        }
    }
}
