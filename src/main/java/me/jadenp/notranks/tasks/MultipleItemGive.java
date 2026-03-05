package me.jadenp.notranks.tasks;

import me.jadenp.notranks.NumberFormatting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MultipleItemGive extends CancelableTask{
    private final Player p;
    private final List<ItemStack> itemStackList;
    private int index = 0;

    public MultipleItemGive(Player p, List<ItemStack> itemStackList) {
        super();
        this.p = p;
        this.itemStackList = itemStackList;
    }
    @Override
    public void cancel() {
        super.cancel();
        if (index < itemStackList.size()) {
            // refund the rest of the items
            for (int i = index; i < itemStackList.size(); i++) {
                me.jadenp.notranks.NumberFormatting.givePlayerInstantly(p, itemStackList.get(i), itemStackList.get(i).getAmount());
            }

                    }
    }

    @Override
    public void run() {
        if (p.isOnline()) {
            NumberFormatting.givePlayer(p, itemStackList.get(index), itemStackList.get(index).getAmount());
            index++;
            if (index >= itemStackList.size())
                this.cancel();
        } else {
            // cancel -> will add to refund
            this.cancel();
        }
    }
}
