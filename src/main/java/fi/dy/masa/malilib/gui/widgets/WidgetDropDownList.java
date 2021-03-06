package fi.dy.masa.malilib.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.mumfrey.liteloader.client.gui.GuiSimpleScrollBar;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.MaLiLibIcons;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;
import fi.dy.masa.malilib.interfaces.IStringRetriever;
import fi.dy.masa.malilib.interfaces.IStringValue;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

/**
 * A dropdown selection widget for entries in the given list.
 * If the entries extend {@link IStringValue}, then the {@link IStringValue#getStringValue()}
 * method is used for the display string, otherwise {@link toString()} is used.
 * @author masa
 *
 * @param <T>
 */
public class WidgetDropDownList<T> extends WidgetBase
{
    protected final GuiSimpleScrollBar scrollBar = new GuiSimpleScrollBar();
    protected final List<T> entries;
    protected final List<T> filteredEntries;
    protected final TextFieldWrapper<GuiTextFieldGeneric> searchBar;
    protected final int maxHeight;
    protected final int maxVisibleEntries;
    protected final int totalHeight;
    protected boolean isOpen;
    protected int selectedIndex;
    protected int scrollbarWidth = 10;
    @Nullable protected final IStringRetriever<T> stringRetriever;
    @Nullable protected T selectedEntry;

    public WidgetDropDownList(int x, int y, int width, int height, int maxHeight,
            int maxVisibleEntries, float zLevel, List<T> entries)
    {
        this(x, y, width, height, maxHeight, maxVisibleEntries, zLevel, entries, null);
    }

    public WidgetDropDownList(int x, int y, int width, int height, int maxHeight,
            int maxVisibleEntries, float zLevel, List<T> entries, @Nullable IStringRetriever<T> stringRetriever)
    {
        super(x, y, width, height, zLevel);

        Minecraft mc = Minecraft.getMinecraft();
        this.width = this.getRequiredWidth(width, entries, mc);
        this.maxHeight = maxHeight;
        this.entries = entries;
        this.filteredEntries = new ArrayList<>();
        this.stringRetriever = stringRetriever;

        ScaledResolution sr = new ScaledResolution(mc);
        int v = Math.min(maxVisibleEntries, entries.size());
        v = Math.min(v, maxHeight / height);
        v = Math.min(v, (sr.getScaledHeight() - y) / height);
        v = Math.max(v, 1);

        this.maxVisibleEntries = v;
        this.totalHeight = (v + 1) * height;
        this.scrollBar.setMaxValue(entries.size() - this.maxVisibleEntries);

        TextFieldListener listener = new TextFieldListener(this);
        this.searchBar = new TextFieldWrapper<>(new GuiTextFieldGeneric(x + 1, y - 18, this.width - 2, 16, mc.fontRenderer), listener);
        this.searchBar.getTextField().setFocused(true);

        this.updateFilteredEntries();
    }

    @Override
    public void setPosition(int x, int y)
    {
        super.setPosition(x, y);

        this.searchBar.getTextField().x = x + 1;
        this.searchBar.getTextField().y = y - 18;
    }

    protected int getRequiredWidth(int width, List<T> entries, Minecraft mc)
    {
        if (width == -1)
        {
            width = 0;

            for (int i = 0; i < entries.size(); ++i)
            {
                width = Math.max(width, mc.fontRenderer.getStringWidth(this.getDisplayString(entries.get(i))) + 20);
            }
        }

        return width;
    }

    @Nullable
    public T getSelectedEntry()
    {
        return this.selectedEntry;
    }

    public WidgetDropDownList<T> setSelectedEntry(T entry)
    {
        if (this.entries.contains(entry))
        {
            this.selectedEntry = entry;
        }

        return this;
    }

    protected void setSelectedEntry(int index)
    {
        if (index >= 0 && index < this.filteredEntries.size())
        {
            this.selectedEntry = this.filteredEntries.get(index);
        }
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY)
    {
        int maxY = this.isOpen ? this.y + this.totalHeight : this.y + this.height;
        return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < maxY;
    }

    @Override
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton)
    {
        if (this.isOpen && mouseY > this.y + this.height)
        {
            if (mouseX < this.x + this.width - this.scrollbarWidth)
            {
                int relIndex = (mouseY - this.y - this.height) / this.height;
                this.setSelectedEntry(this.scrollBar.getValue() + relIndex);
            }
            else
            {
                if (this.scrollBar.wasMouseOver() == false)
                {
                    int relY = mouseY - this.y - this.height;
                    int ddHeight = this.height * this.maxVisibleEntries;
                    int newPos = (int) (((double) relY / (double) ddHeight) * this.scrollBar.getMaxValue());

                    this.scrollBar.setValue(newPos);
                    this.scrollBar.handleDrag(mouseY, 123);
                }

                this.scrollBar.setDragging(true);
            }
        }

        if (this.isOpen == false || (mouseX < this.x + this.width - this.scrollbarWidth || mouseY < this.y + this.height))
        {
            this.isOpen = ! this.isOpen;

            if (this.isOpen == false)
            {
                this.searchBar.getTextField().setText("");
                this.updateFilteredEntries();
            }
        }

        return true;
    }

    @Override
    public void onMouseReleasedImpl(int mouseX, int mouseY, int mouseButton)
    {
        this.scrollBar.setDragging(false);
    }

    @Override
    public boolean onMouseScrolledImpl(int mouseX, int mouseY, int mouseWheelDelta)
    {
        if (this.isOpen)
        {
            int amount = mouseWheelDelta < 0 ? 1 : -1;
            this.scrollBar.offsetValue(amount);
        }

        return false;
    }

    @Override
    protected boolean onKeyTypedImpl(char typedChar, int keyCode)
    {
        if (this.isOpen)
        {
            return this.searchBar.keyTyped(typedChar, keyCode);
        }

        return false;
    }

    protected void updateFilteredEntries()
    {
        this.filteredEntries.clear();
        String filterText = this.searchBar.getTextField().getText();

        if (this.isOpen && filterText.isEmpty() == false)
        {
            for (int i = 0; i < this.entries.size(); ++i)
            {
                T entry = this.entries.get(i);

                if (this.entryMatchesFilter(entry, filterText))
                {
                    this.filteredEntries.add(entry);
                }
            }

            this.scrollBar.setValue(0);
        }
        else
        {
            this.filteredEntries.addAll(this.entries);
        }

        this.scrollBar.setMaxValue(this.filteredEntries.size() - this.maxVisibleEntries);
    }

    protected boolean entryMatchesFilter(T entry, String filterText)
    {
        return filterText.isEmpty() || this.getDisplayString(entry).toLowerCase().indexOf(filterText) != -1;
    }

    protected String getDisplayString(T entry)
    {
        if (entry != null)
        {
            if (this.stringRetriever != null)
            {
                return this.stringRetriever.getStringValue(entry);
            }

            return entry.toString();
        }

        return "-";
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        GlStateManager.color(1f, 1f, 1f, 1f);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 1);
        List<T> list = this.filteredEntries;
        int visibleEntries = Math.min(this.maxVisibleEntries, list.size());

        RenderUtils.drawOutlinedBox(this.x + 1, this.y, this.width - 2, this.height - 1, 0xFF101010, 0xFFC0C0C0);

        String str = this.getDisplayString(this.getSelectedEntry());
        int fh = this.mc.fontRenderer.FONT_HEIGHT;
        int txtX = this.x + 4;
        int txtY = this.y + this.height / 2 - fh / 2;
        this.mc.fontRenderer.drawString(str, txtX, txtY, 0xFFE0E0E0);
        txtY += this.height + 1;
        int scrollWidth = 10;

        if (this.isOpen)
        {
            if (this.searchBar.getTextField().getText().isEmpty() == false)
            {
                this.searchBar.draw();
            }

            RenderUtils.drawOutline(this.x, this.y + this.height, this.width, visibleEntries * this.height + 2, 0xFFE0E0E0);

            int y = this.y + this.height + 1;
            int startIndex = Math.max(0, this.scrollBar.getValue());
            int max = Math.min(startIndex + this.maxVisibleEntries, list.size());

            for (int i = startIndex; i < max; ++i)
            {
                int bg = (i & 0x1) != 0 ? 0x20FFFFFF : 0x30FFFFFF;

                if (mouseX >= this.x && mouseX < this.x + this.width - scrollWidth &&
                    mouseY >= y && mouseY < y + this.height)
                {
                    bg = 0x60FFFFFF;
                }

                RenderUtils.drawRect(this.x, y, this.width - scrollWidth, this.height, bg);
                str = this.getDisplayString(list.get(i));
                this.mc.fontRenderer.drawString(str, txtX, txtY, 0xFFE0E0E0);
                y += this.height;
                txtY += this.height;
            }

            int x = this.x + this.width - this.scrollbarWidth - 1;
            y = this.y + this.height + 1;
            int h = visibleEntries * this.height;
            int totalHeight = Math.max(h, list.size() * this.height);

            this.scrollBar.drawScrollBar(mouseX, mouseY, 0, x, y, this.scrollbarWidth, h, totalHeight);
        }
        else
        {
            this.mc.getTextureManager().bindTexture(MaLiLibIcons.TEXTURE);
            MaLiLibIcons i = MaLiLibIcons.ARROW_DOWN;
            RenderUtils.drawTexturedRect(this.x + this.width - 16, this.y + 2, i.getU() + i.getWidth(), i.getV(), i.getWidth(), i.getHeight());
        }

        GlStateManager.popMatrix();
    }

    protected static class TextFieldListener implements ITextFieldListener<GuiTextFieldGeneric>
    {
        protected final WidgetDropDownList<?> widget;

        protected TextFieldListener(WidgetDropDownList<?> widget)
        {
            this.widget = widget;
        }

        @Override
        public boolean onTextChange(GuiTextFieldGeneric textField)
        {
            this.widget.updateFilteredEntries();
            return true;
        }
    }
}
