package com.mercateo.ops.tools.springbootstatus;

class StatusBean {
    @Override
    public String toString() {
        return "StatusBean [status=" + this.status + "]";
    }

    String status = "unknown";

    public boolean isUp() {
        return this.status.equalsIgnoreCase("up");
    }
}
