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

public class App {
    private final static Logger logger = LoggerFactory.getLogger(App.class);
    private final static String version = App.class.getPackage().getImplementationVersion();

    private static String inFile = null;
    private static String outFile = null;
    private static int outSel = -1;
    private static boolean useSourceFIOs = false;

    public static void main(String[] args) throws IOException {
        System.out.println("DuPAL Espresso Converter " + version);

        if (args.length < 2) {
            logger.error("Wrong number of arguments passed.\n"
                    + "dupal_espressoconverter <input_file> <output_file> [single output table] [use only source FIOs Y|N]\n");

            return;
        }

        parseArgs(args);

        JSONObject root = new JSONObject(new JSONTokener(new FileReader(inFile)));
        PALSpecs pSpecs = ContentParser.getPALType(root);

        if(pSpecs == null) {
            logger.error("Unknown PAL type for file.");
            return;
        }
        
        logger.info("Got file for PAL type " + pSpecs);

        if(outSel >= 0) logger.info("Printing table only for output number " + outSel);

        String header = null;
        String[][] tables = null;
        String footer = EspressoFormatter.formatEspressoFooter();

        if(pSpecs.getPinCount_IO() > 0 || pSpecs.getPinCount_RO() > 0) {
            int IOsAsOUTs = ContentParser.extractIOasOutMask(root);
            RLink[] rlArray = ContentParser.extractRLinks(root);
            OLink[] olArray = ContentParser.extractOLinks(root);
            
            header = EspressoFormatter.formatEspressoTableHeader(pSpecs, IOsAsOUTs, outSel);
            tables = EspressoFormatter.formatEspressoTable(pSpecs, IOsAsOUTs, olArray, rlArray, outSel, useSourceFIOs);

        } else {
            SimpleState[] ssArray = ContentParser.extractSimpleStates(root);
            header = EspressoFormatter.formatEspressoTableHeader(pSpecs, 0, outSel);
            tables = EspressoFormatter.formatEspressoTable(pSpecs, ssArray);
        }

        saveTableToFile(outFile, header, tables, footer);
    }

    private static void parseArgs(String[] args) {
        inFile = args[0];
        outFile = args[1];

        if(args.length >= 3) {
            outSel = Integer.parseInt(args[2]);
        }

        if(args.length >= 4) {
            useSourceFIOs = args[3].equalsIgnoreCase("Y");
        }
    }

    private static void saveTableToFile(String destination, String header, String[][] tables, String footer) throws IOException {
        FileOutputStream fout = null;
        int file_counter = 0;        

        for(String[] table : tables) {
            if(table != null && table.length > 0) {
                if(tables.length > 1) {
                    logger.info("saveOutputToFile() -> Saving to " + destination+".tab"+file_counter);
                    fout = new FileOutputStream(destination+".tab"+file_counter);
                } else {
                    logger.info("saveOutputToFile() -> Saving to " + destination);
                    fout = new FileOutputStream(destination);
                }

                fout.write(header.getBytes(StandardCharsets.US_ASCII));
                for(String row : table) fout.write(row.getBytes(StandardCharsets.US_ASCII));  
                fout.write(footer.getBytes(StandardCharsets.US_ASCII));
                
                fout.flush();
                fout.close();

                file_counter++;
            }
        }
    }
}
