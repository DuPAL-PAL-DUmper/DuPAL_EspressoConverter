package info.hkzlab.dupal.EspressoConverter;

import org.junit.Test;
import static org.junit.Assert.*;

import info.hkzlab.dupal.EspressoConverter.devices.PAL16L8Specs;
import info.hkzlab.dupal.EspressoConverter.formatter.EspressoFormatter;
import info.hkzlab.dupal.EspressoConverter.states.OLink;
import info.hkzlab.dupal.EspressoConverter.states.OutStatePins;
import info.hkzlab.dupal.EspressoConverter.states.RLink;

public class EspressoFormatterTest
{
    @Test
    public void espressoFormatterShouldBuildCorrect16L8Header() {	
        PAL16L8Specs pSpecs = new PAL16L8Specs();	
        int ioAsOutMask = 0x03;	
        String header = EspressoFormatter.formatEspressoTableHeader(pSpecs, ioAsOutMask, -1);	
        String expectedHeader = "# PAL16L8\n" + ".i 16\n" + ".o 8\n"	
                + ".ilb i1 i2 i3 i4 i5 i6 i7 i8 i9 i11 io16 io15 io14 io13 fio18 fio17 \n"	
                + ".ob o19 o12 io18 io17 o19oe o12oe io18oe io17oe \n" + ".phase 00000000\n\n";	

        assertEquals("EspressoFormatter should build a correct 16L8 header", expectedHeader, header);	
    }

    @Test
    public void espressoFormatterShouldBuildCorrectSingleOutput16L8Header() {
        PAL16L8Specs pSpecs = new PAL16L8Specs();	
        int ioAsOutMask = 0x03;	
        String header = EspressoFormatter.formatEspressoTableHeader(pSpecs, ioAsOutMask, 2);	
        String expectedHeader = "# PAL16L8\n" + ".i 16\n" + ".o 1\n"	
                + ".ilb i1 i2 i3 i4 i5 i6 i7 i8 i9 i11 io16 io15 io14 io13 fio18 fio17 \n"	
                + ".ob io18 \n" + ".phase 0\n\n";	

        assertEquals("EspressoFormatter should build a correct 16L8 header when a single output is selected", expectedHeader, header);	        
    }

    @Test
    public void espressoFormatterShouldBuildCorrect16L8TableWithAllOutputs() {
        PAL16L8Specs pSpecs = new PAL16L8Specs();	
        int ioAsOutMask = 0x38;	

        OLink[] oLinks = new OLink[9];

        oLinks[0] = new OLink(0x1FFF, new OutStatePins(0x00, 0xC0), new OutStatePins(0x38, 0xC0));
        oLinks[1] = new OLink(0x1F3F, new OutStatePins(0x00, 0xC0), new OutStatePins(0xB0, 0x00));
        oLinks[2] = new OLink(0x0000, new OutStatePins(0x00, 0xC0), new OutStatePins(0x28, 0x40));

        oLinks[3] = new OLink(0x1FFF, new OutStatePins(0x38, 0xC0), new OutStatePins(0x28, 0x40));
        oLinks[4] = new OLink(0x1F3F, new OutStatePins(0x38, 0xC0), new OutStatePins(0x00, 0xC0));
        oLinks[5] = new OLink(0x0000, new OutStatePins(0x38, 0xC0), new OutStatePins(0xB0, 0x00));
        
        oLinks[6] = new OLink(0x1FFF, new OutStatePins(0xB0, 0x00), new OutStatePins(0x00, 0xC0));
        oLinks[7] = new OLink(0x1F3F, new OutStatePins(0xB0, 0x00), new OutStatePins(0x38, 0xC0));
        oLinks[8] = new OLink(0x0000, new OutStatePins(0xB0, 0x00), new OutStatePins(0x38, 0xC0));
        
        String[] rows = EspressoFormatter.formatEspressoTable(pSpecs, ioAsOutMask, oLinks, new RLink[0], -1, false);

        String[] expected = new String[] {	
            "0000000000000000 -010110000\n",
            "0000000000000011 --11111000\n",
            "0000000000000111 0101100000\n",
            "1111110011111000 0101100000\n",
            "1111110011111011 --11111000\n",
            "1111110011111111 --00011000\n",
            "1111111111111000 --11111000\n",
            "1111111111111011 --00011000\n",
            "1111111111111111 -010110000\n"
        };	

        assertArrayEquals("EspressoFormatter should build the correct truth table for specified states", expected, rows);	
    }
}
