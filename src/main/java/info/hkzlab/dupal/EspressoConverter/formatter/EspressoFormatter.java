package info.hkzlab.dupal.EspressoConverter.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.hkzlab.dupal.EspressoConverter.devices.PALSpecs;
import info.hkzlab.dupal.EspressoConverter.states.*;
import info.hkzlab.dupal.EspressoConverter.utilities.BitUtils;

public class EspressoFormatter {
    private final static Logger logger = LoggerFactory.getLogger(EspressoFormatter.class);

    private EspressoFormatter() {};

    public static String formatEspressoTableHeader(PALSpecs pSpecs, int ioAsOutMask, int singleOutSelection) {
        StringBuffer strBuf = new StringBuffer();
        int ioAsOut_W = BitUtils.scatterBitField(BitUtils.consolidateBitField(ioAsOutMask, pSpecs.getMask_IO_R()), pSpecs.getMask_IO_W());
        int io_outCount = BitUtils.countBits(ioAsOutMask);
        int io_inCount = BitUtils.countBits(pSpecs.getMask_IO_R() & ~ioAsOutMask);
        
        int outCount = pSpecs.getPinCount_O() + io_outCount + pSpecs.getPinCount_RO();
        int outCount_oe = pSpecs.getPinCount_O() + io_outCount;
        int inCount = pSpecs.getPinCount_IN() + io_inCount + io_outCount + pSpecs.getPinCount_RO();

        strBuf.append("# " + pSpecs.toString() + "\n");
        strBuf.append(".i " + inCount + "\n"); // Inputs, IO as inputs, IO as outputs (as feedbacks), registered outputs (as feedbacks)
        
        if(singleOutSelection >= 0) strBuf.append(".o 1\n");
        else strBuf.append(".o " + (outCount_oe + outCount) + "\n"); // Outputs, IO as outputs, Registered Outputs, then an out for all of those as OE
        
        strBuf.append(".ilb ");
        for(int idx = 0; idx < 32; idx++) if(((pSpecs.getMask_IN() >> idx) & 0x01) > 0) strBuf.append(pSpecs.getLabels_IN()[idx] + " ");
        for(int idx = 0; idx < 32; idx++) if((((pSpecs.getMask_IO_W() & ~ioAsOut_W) >> idx) & 0x01) > 0) strBuf.append(pSpecs.getLabels_IO()[idx] + " ");
        for(int idx = 0; idx < 32; idx++) if((((pSpecs.getMask_IO_W() & ioAsOut_W) >> idx) & 0x01) > 0) strBuf.append("f" + pSpecs.getLabels_IO()[idx] + " ");
        for(int idx = 0; idx < 32; idx++) if(((pSpecs.getMask_RO_W() >> idx) & 0x01) > 0) strBuf.append("ps"+pSpecs.getLabels_RO()[idx] + " ");
        strBuf.append("\n");
        
        strBuf.append(".ob ");
        ArrayList<String> obLabels = new ArrayList<>();
        for(int idx = 0; idx < 32; idx++) if(((pSpecs.getMask_O_W() >> idx) & 0x01) > 0) obLabels.add(pSpecs.getLabels_O()[idx] + " ");
        for(int idx = 0; idx < 32; idx++) if((((pSpecs.getMask_IO_W() & ioAsOut_W) >> idx) & 0x01) > 0) obLabels.add(pSpecs.getLabels_IO()[idx] + " ");
        for(int idx = 0; idx < 32; idx++) if(((pSpecs.getMask_RO_W() >> idx) & 0x01) > 0) obLabels.add(pSpecs.getLabels_RO()[idx] + " ");
        for(int idx = 0; idx < 32; idx++) if(((pSpecs.getMask_O_W() >> idx) & 0x01) > 0) obLabels.add(pSpecs.getLabels_O()[idx] + "oe ");
        for(int idx = 0; idx < 32; idx++) if((((pSpecs.getMask_IO_W() & ioAsOut_W) >> idx) & 0x01) > 0) obLabels.add(pSpecs.getLabels_IO()[idx] + "oe ");
        
        if(singleOutSelection >= 0) strBuf.append(obLabels.get(singleOutSelection));
        else for(String label : obLabels) strBuf.append(label);

        strBuf.append("\n");
       
        strBuf.append(".phase ");
        ArrayList<Character> phaseLabels = new ArrayList<>();
        for(int idx = 0; idx < outCount; idx++) phaseLabels.add(pSpecs.isActiveLow() ? '0' : '1');
        for(int idx = 0; idx < outCount_oe; idx++) phaseLabels.add('0');

        if(singleOutSelection >= 0) strBuf.append(phaseLabels.get(singleOutSelection));
        else for(char phase : phaseLabels) strBuf.append(phase);
        strBuf.append("\n\n");
        
        logger.info("formatEspressoTableHeader() -> Will format a table for a " + pSpecs + ", IO mask " + String.format("%02X", ioAsOutMask) + (singleOutSelection >= 0 ? " - Will print only output " + obLabels.get(singleOutSelection) + "out of " + (outCount + outCount_oe) : ""));
        
        return strBuf.toString();
    }
  
    public static String[][] formatEspressoTable(PALSpecs pSpecs, int ioAsOutMask, OLink[] oLinks, RLink[] rLinks) {
        return formatEspressoTable(pSpecs, ioAsOutMask, oLinks, rLinks, -1, false);
    }
    
    public static String[][] formatEspressoTable(PALSpecs pSpecs, int ioAsOutMask, OLink[] oLinks, RLink[] rLinks, int singleOutSelection, boolean useSourceFIOs) {
        if(useSourceFIOs) logger.info("formatEspressoTable() -> Feedback IOs will be taken from the source state only!");

        int ioAsOut_W = BitUtils.scatterBitField(BitUtils.consolidateBitField(ioAsOutMask, pSpecs.getMask_IO_R()), pSpecs.getMask_IO_W());
        HashSet<String> olTableRows = new HashSet<>();
        HashSet<String> rlTableRows = new HashSet<>();

        int ins, io_ins, io_fio, io_fio_hiz, io_fio_dst, io_fio_hiz_dst, ro_ps, outs, outs_hiz, io_outs, io_outs_hiz, ro;
        int io_ins_count = BitUtils.countBits(pSpecs.getMask_IO_W() & ~ioAsOut_W);
        int io_fio_count = BitUtils.countBits(pSpecs.getMask_IO_R() & ioAsOutMask);
        int io_outs_count = BitUtils.countBits(ioAsOutMask);

        StringBuffer strBuf = new StringBuffer();

        for(OLink ol : oLinks) {
            strBuf.delete(0, strBuf.length());

            ins = BitUtils.consolidateBitField(ol.inputs, pSpecs.getMask_IN()); // inputs
            io_ins = BitUtils.consolidateBitField(ol.inputs, pSpecs.getMask_IO_W() & ~ioAsOut_W); // IOs as inputs
            io_fio = BitUtils.consolidateBitField(ol.src.out, ioAsOutMask); // IO as outputs (feedbacks)
            io_fio_hiz = BitUtils.consolidateBitField(ol.src.hiz, ioAsOutMask); // IO as outputs (feedbacks) - hiz flags
            io_fio_dst = BitUtils.consolidateBitField(ol.dst.out, ioAsOutMask); // IO as outputs (feedbacks) coming from destination state
            io_fio_hiz_dst = BitUtils.consolidateBitField(ol.dst.hiz, ioAsOutMask); // IO as outputs (feedbacks) coming from destination state - hiz flags

            ro_ps = BitUtils.consolidateBitField(ol.src.out, pSpecs.getMask_RO_R()); // Old Registered Outputs

            outs = BitUtils.consolidateBitField(ol.dst.out, pSpecs.getMask_O_R()); // Outputs
            outs_hiz = BitUtils.consolidateBitField(ol.dst.hiz, pSpecs.getMask_O_R()); // Outputs - hiz flags
            io_outs = BitUtils.consolidateBitField(ol.dst.out, ioAsOutMask); // IO as outputs (feedbacks)
            io_outs_hiz = BitUtils.consolidateBitField(ol.dst.hiz, ioAsOutMask); // IO as outputs (feedbacks)
            ro = 0x00; // We'll set these as "don't care" for this type of link, as they can only be changed via a registered link

            // Print the inputs
            int in_pin_cnt = 0;
            int singleOutSelectionInput = -1;

            // In case the selected single output to print corresponds to a feedback io, calculate which one we need to fetch from the source state
            if(singleOutSelection >= pSpecs.getPinCount_O()) {
                singleOutSelectionInput = pSpecs.getPinCount_IN() + (pSpecs.getPinCount_IO() - BitUtils.countBits(ioAsOutMask)) + (singleOutSelection - pSpecs.getPinCount_O());
                System.out.println("singleOutSelectionInput " + singleOutSelectionInput);
            }

            for(int idx = 0; idx < pSpecs.getPinCount_IN(); idx++, in_pin_cnt++) strBuf.append((char)(((ins >> idx) & 0x01) + 0x30));
            for(int idx = 0; idx < io_ins_count; idx++, in_pin_cnt++) strBuf.append((char)(((io_ins >> idx) & 0x01) + 0x30));
            for(int idx = 0; idx < io_fio_count; idx++, in_pin_cnt++) {
                if(useSourceFIOs || (in_pin_cnt == singleOutSelectionInput) || (singleOutSelection < 0)) {
                    boolean fio_pin_hiz = ((io_fio_hiz >> idx) & 0x01) != 0;
                    strBuf.append(fio_pin_hiz ? '-' : (char)(((io_fio >> idx) & 0x01) + 0x30));
                } else {
                    boolean fio_pin_hiz_dst = ((io_fio_hiz_dst >> idx) & 0x01) != 0;
                    strBuf.append(fio_pin_hiz_dst ? '-' : (char)(((io_fio_dst >> idx) & 0x01) + 0x30));                       
                }
            }
            for(int idx = 0; idx < pSpecs.getPinCount_RO(); idx++) strBuf.append((char)(((ro_ps >> idx) & 0x01) + 0x30));

            strBuf.append(' ');
            ArrayList<Character> outArray = new ArrayList<>();
            // Print the outputs
            for(int idx = 0; idx < pSpecs.getPinCount_O(); idx++) {
                boolean out_pin_hiz = ((outs_hiz >> idx) & 0x01) != 0;
                outArray.add(out_pin_hiz ? '-' : (char)(((outs >> idx) & 0x01) + 0x30));
            }
            for(int idx = 0; idx < io_outs_count; idx++) {
                boolean io_pin_hiz = ((io_outs_hiz >> idx) & 0x01) != 0;
                outArray.add(io_pin_hiz ? '-' : (char)(((io_outs >> idx) & 0x01) + 0x30));
            }
            for(int idx = 0; idx < pSpecs.getPinCount_RO(); idx++) outArray.add('-'); // Ignore the destination Registered Outputs for this type of link, as they can't change
            // Print the outputs (hiz flags)
            for(int idx = 0; idx < pSpecs.getPinCount_O(); idx++) outArray.add((char)(((outs_hiz >> idx) & 0x01) + 0x30));
            for(int idx = 0; idx < io_outs_count; idx++) outArray.add((char)(((io_outs_hiz >> idx) & 0x01) + 0x30));

            if(singleOutSelection >= 0) strBuf.append(outArray.get(singleOutSelection));
            else for(char out : outArray) strBuf.append(out);
            strBuf.append('\n');

            olTableRows.add(strBuf.toString());
        }

        for(RLink rl : rLinks) {
            strBuf.delete(0, strBuf.length());

            ins = BitUtils.consolidateBitField(rl.inputs, pSpecs.getMask_IN()); // inputs
            io_ins = BitUtils.consolidateBitField(rl.inputs, pSpecs.getMask_IO_W() & ~ioAsOut_W); // IOs as inputs
            io_fio = BitUtils.consolidateBitField(rl.middle.out, pSpecs.getMask_IO_R() & ioAsOutMask); // IO as outputs (feedbacks)
            io_fio_hiz = BitUtils.consolidateBitField(rl.middle.hiz, pSpecs.getMask_IO_R() & ioAsOutMask); // IO as outputs (feedbacks) - hiz flags
            ro_ps = BitUtils.consolidateBitField(rl.middle.out, pSpecs.getMask_RO_R()); // Old Registered Outputs

            outs = 0x00; // Outputs, Ignore, we'll set them as don't care for this type of link, these will be set by outlinks
            outs_hiz = 0x00;
            io_outs = 0x00;
            io_outs_hiz = 0x00; 
            ro = BitUtils.consolidateBitField(rl.dst.out, pSpecs.getMask_RO_R()); // Registered outputs

            // Print the inputs
            for(int idx = 0; idx < pSpecs.getPinCount_IN(); idx++) strBuf.append((char)(((ins >> idx) & 0x01) + 0x30));
            for(int idx = 0; idx < io_ins_count; idx++) strBuf.append((char)(((io_ins >> idx) & 0x01) + 0x30));

            for(int idx = 0; idx < io_fio_count; idx++) {
                boolean fio_pin_hiz = ((io_fio_hiz >> idx) & 0x01) != 0;
                strBuf.append(fio_pin_hiz ? '-' : (char)(((io_fio >> idx) & 0x01) + 0x30));
            }

            for(int idx = 0; idx < pSpecs.getPinCount_RO(); idx++) strBuf.append((char)(((ro_ps >> idx) & 0x01) + 0x30));
              
            strBuf.append(' ');
            ArrayList<Character> outArray = new ArrayList<>();
            // Print the outputs
            for(int idx = 0; idx < pSpecs.getPinCount_O(); idx++) outArray.add('-');
            for(int idx = 0; idx < io_outs_count; idx++) outArray.add('-');
            for(int idx = 0; idx < pSpecs.getPinCount_RO(); idx++)outArray.add((char)(((ro >> idx) & 0x01) + 0x30));
            // Print the outputs (hiz flags)
            for(int idx = 0; idx < pSpecs.getPinCount_O()+io_outs_count; idx++) outArray.add('-');
            
            if(singleOutSelection >= 0) strBuf.append(outArray.get(singleOutSelection));
            else for(char out : outArray) strBuf.append(out);
            strBuf.append('\n');
            
            rlTableRows.add(strBuf.toString());
        }

        String[] olTableArray = olTableRows.toArray(new String[olTableRows.size()]);
        String[] rlTableArray = rlTableRows.toArray(new String[rlTableRows.size()]);

        Arrays.sort(olTableArray);
        Arrays.sort(rlTableArray);

        ArrayList<String[]> tabArray = new ArrayList<>();

        if(olTableArray != null && olTableArray.length > 0) tabArray.add(olTableArray);
        if(rlTableArray != null && rlTableArray.length > 0) tabArray.add(rlTableArray);

        return tabArray.toArray(new String[tabArray.size()][]);
    }

    public static String[][] formatEspressoTable(PALSpecs pSpecs, SimpleState[] states) {
        ArrayList<String> tableRows = new ArrayList<>();

        StringBuffer strBuf = new StringBuffer();
        for(SimpleState ss : states) {
            strBuf.delete(0, strBuf.length());
            
            int inputs = BitUtils.consolidateBitField(ss.inputs, pSpecs.getMask_IN());
            int output = BitUtils.consolidateBitField(ss.outputs.out, pSpecs.getMask_O_R());
            int hiz = BitUtils.consolidateBitField(ss.outputs.hiz, pSpecs.getMask_O_R());
            for(int idx = 0; idx < pSpecs.getPinCount_IN(); idx++) strBuf.append((char)(((inputs >> idx) & 0x01) + 0x30));
            strBuf.append(' ');
            for(int idx = 0; idx < pSpecs.getPinCount_O(); idx++) {
                boolean hiz_pin = ((hiz >> idx) & 0x01) != 0;
                strBuf.append(hiz_pin ? '-' : (char)(((output >> idx) & 0x01) + 0x30));
            }
            for(int idx = 0; idx < pSpecs.getPinCount_O(); idx++) strBuf.append((char)(((hiz >> idx) & 0x01) + 0x30));
            strBuf.append('\n');

            tableRows.add(strBuf.toString());
        }

        String[] tableRowsArray = tableRows.toArray(new String[tableRows.size()]);
        Arrays.sort(tableRowsArray);

        return new String[][] { tableRowsArray };
    }

    public static String formatEspressoFooter() {
        return ".e\n\n";
    }
}
