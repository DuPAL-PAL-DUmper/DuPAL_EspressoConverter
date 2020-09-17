package info.hkzlab.dupal.EspressoConverter.states;

public class OLink {
    public final int inputs;
    public final OutStatePins src, dst;

    public OLink(final int inputs, final OutStatePins src, final OutStatePins dst) {
        this.inputs = inputs;
        this.src = src;
        this.dst = dst;
    }
}
