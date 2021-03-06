package fi.dy.masa.malilib.util.restrictions;

import java.util.List;
import java.util.Set;
import fi.dy.masa.malilib.LiteModMaLiLib;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class ItemRestriction extends UsageRestriction<Item>
{
    @Override
    protected void setValuesForList(Set<Item> set, List<String> names)
    {
        for (String name : names)
        {
            Item item = Item.REGISTRY.getObject(new ResourceLocation(name));

            if (item != null)
            {
                set.add(item);
            }
            else
            {
                LiteModMaLiLib.logger.warn(I18n.format("malilib.error.invalid_item_blacklist_entry", name));
            }
        }
    }
}
