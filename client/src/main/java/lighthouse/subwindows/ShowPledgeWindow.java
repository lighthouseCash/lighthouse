package lighthouse.subwindows;

import javafx.application.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import lighthouse.*;
import lighthouse.protocol.*;
import lighthouse.wallet.*;
import org.bitcoinj.core.*;

import java.time.*;
import java.time.format.*;

/**
 * Allows the user to view the details of the pledge and copy/paste the
 */
public class ShowPledgeWindow {
    @FXML Label amountLabel;
    @FXML Label contactLabel;
    @FXML Label dateLabel;
    @FXML TextArea messageField;
    @FXML Button saveToFileButton;

    public Main.OverlayUI<ShowPledgeWindow> overlayUI;

    private Project project;   // if our pledge in serverless mode
    private LHProtos.Pledge pledge;

    public static Main.OverlayUI<ShowPledgeWindow> open(Project project, LHProtos.Pledge pledge) {
        Main.OverlayUI<ShowPledgeWindow> ui = Main.instance.overlayUI("subwindows/show_pledge.fxml", "View pledge");
        ui.controller.init(project, pledge);
        return ui;
    }

    private void init(Project project, LHProtos.Pledge pledge) {
        amountLabel.setText(Coin.valueOf(pledge.getPledgeDetails().getTotalInputValue()).toFriendlyString());
        if (pledge.hasPledgeDetails()) {
            contactLabel.setText(pledge.getPledgeDetails().getContactAddress());
            // Looks like an email address?
            if (contactLabel.getText().matches("[a-zA-Z0-9\\._]+@[^ ]+")) {
                contactLabel.setStyle(contactLabel.getStyle() + "; -fx-cursor: hand; -fx-text-fill: blue; -fx-underline: true");
                contactLabel.setOnMouseClicked(ev -> Main.instance.getHostServices().showDocument(String.format("mailto:%s", contactLabel.getText())));
            }
            messageField.setText(pledge.getPledgeDetails().getMemo());
        } else {
            contactLabel.setText("<unknown>");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM uuuu HH:mm");
        LocalDateTime time = LocalDateTime.ofEpochSecond(pledge.getPledgeDetails().getTimestamp(), 0, ZoneOffset.UTC);
        dateLabel.setText(time.format(formatter));
        this.project = project;
        this.pledge = pledge;

        // Let the user save their pledge again if it's a serverless project and this pledge is ours.
        LHProtos.Pledge pledgeFor = Main.wallet.getPledgeFor(project);
        saveToFileButton.setVisible(project.getPaymentURL() == null && pledgeFor != null && LHUtils.hashFromPledge(pledge).equals(LHUtils.hashFromPledge(pledgeFor)));
    }

    @FXML
    public void closeClicked(ActionEvent event) {
        overlayUI.done();
    }

    @FXML
    public void saveToFile(ActionEvent event) {
        Platform.runLater(() -> {
            ExportWindow.openForPledge(project, new PledgingWallet.PledgeSupplier() {
                @Override
                public LHProtos.Pledge getData() {
                    return pledge;
                }

                @Override
                public LHProtos.Pledge commit(boolean andBroadcastDeps) {
                    return null;   // Doesn't matter.
                }
            });
        });
    }
}
