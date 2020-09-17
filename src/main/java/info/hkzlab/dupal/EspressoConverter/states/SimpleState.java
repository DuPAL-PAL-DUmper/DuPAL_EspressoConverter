package info.hkzlab.dupal.EspressoConverter.states;

public class SimpleState {
    public final int inputs;
    public final OutStatePins outputs;

    public SimpleState(final int inputs, final int outs, final int hiz) {
        this.inputs = inputs;
        outputs = new OutStatePins(outs, hiz);
    }
}
