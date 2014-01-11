
package info.guardianproject.gpg;

public class KeyserverResult<D> {
    private int errorResid = 0;
    private D data;

    public int getErrorResid() {
        return errorResid;
    }

    public void setErrorResid(int errorResid) {
        this.errorResid = errorResid;
    }

    public void setData(D data) {
        this.data = data;
    }

    public D getData() {
        return data;
    }
}
