package com.mercateo.ops.tools.springbootstatus;

class StatusBean {

    private static final String KEY = "status=";

    private static final String ACTUATOR_STATUS_UP = "UP";

    private static final String ACTUATOR_STATUS_UNKNOWN_ABBR = "UN";

    private final String status;

    StatusBean(String status) {
        this.status = status;
    }

    public boolean isUp() {
        if (null == status || status.trim().length() == 0) {
            return false;
        }

        int idx = status.indexOf(KEY);
        if (idx < 0) {
            return false;
        }
        boolean up = true;
        while (idx >= 0) {
            int idxValStart = idx + KEY.length();
            int idxValEnd = idxValStart + 2;

            String val = status.substring(idxValStart, idxValEnd);

            /*
             * Actuator reports "UP" even if one or more are "UNKNOWN". (At
             * least with 1.3.5.RELEASE)
             */
            if (!ACTUATOR_STATUS_UP.equals(val) && !ACTUATOR_STATUS_UNKNOWN_ABBR.equals(val)) {
                up = false;
                break;
            }

            idx = status.indexOf(KEY, idxValEnd);
        }

        return up;
    }

    @Override
    public String toString() {
        return "StatusBean [status=" + (isUp() ? ACTUATOR_STATUS_UP : "UNKNOWN") + "]";
    }

}
