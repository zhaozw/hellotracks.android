package com.hellotracks.account;

import com.hellotracks.billing.PlanHolder;
import com.hellotracks.billing.util.Inventory;

public interface IUpsell {
    public void upsell();
    public void checkout();
    public void setSelectedPlan(PlanHolder planHolder);
    public void finish();
    public Inventory getInventory();
}
