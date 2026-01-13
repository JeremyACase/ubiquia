package org.ubiquia.common.model.ubiquia.node.backpressure;

/**
 * A model representing "back pressure" in a flow.
 */
public class BackPressure {

    private Ingress ingress;

    private Egress egress;

    public Ingress getIngress() {
        return ingress;
    }

    public void setIngress(Ingress ingress) {
        this.ingress = ingress;
    }

    public Egress getEgress() {
        return egress;
    }

    public void setEgress(Egress egress) {
        this.egress = egress;
    }
}
