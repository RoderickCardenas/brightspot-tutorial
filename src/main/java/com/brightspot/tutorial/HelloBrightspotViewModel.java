package com.brightspot.tutorial;
import java.util.Optional;
import com.psddev.cms.view.PageEntryView;
import com.psddev.cms.view.ViewInterface;
import com.psddev.cms.view.ViewModel;

@ViewInterface
public class HelloBrightspotViewModel extends ViewModel<HelloBrightspot> implements PageEntryView {

    public String getMessage() {
        return "Hello " + Optional.ofNullable(model.getName())
            .orElse("Brightspot");
    }
}
