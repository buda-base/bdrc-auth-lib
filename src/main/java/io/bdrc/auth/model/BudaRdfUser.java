package io.bdrc.auth.model;

public class BudaRdfUser {

    public String BudaUserId;
    public String auth0Id;
    public String label;

    public BudaRdfUser(String budaUserId, String auth0Id, String label) {
        super();
        BudaUserId = budaUserId;
        this.auth0Id = auth0Id;
        this.label = label;
    }

    public String getBudaUserId() {
        return BudaUserId;
    }

    public String getAuth0Id() {
        return auth0Id;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "BudaRdfUser [BudaUserId=" + BudaUserId + ", auth0Id=" + auth0Id + ", label=" + label + "]";
    }

}
