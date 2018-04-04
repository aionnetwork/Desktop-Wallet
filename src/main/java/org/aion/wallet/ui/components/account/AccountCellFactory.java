package org.aion.wallet.ui.components.account;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import org.aion.wallet.dto.AccountDTO;

public class AccountCellFactory implements Callback<ListView<AccountDTO>, ListCell<AccountDTO>> {

    @Override
    public ListCell<AccountDTO> call(ListView<AccountDTO> param) {
        return new AccountCellItem();
    }
}
