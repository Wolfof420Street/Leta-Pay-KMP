/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package cmp.navigation.authenticatednavbar

import androidx.compose.ui.graphics.vector.ImageVector
import cmp.navigation.generated.resources.Res
import cmp.navigation.generated.resources.chat_tab
import cmp.navigation.generated.resources.trade_tab
import cmp.navigation.generated.resources.wallet_tab
import cmp.navigation.generated.resources.yield_tab
import cmp.navigation.utils.toObjectNavigationRoute
import com.letapay.app.core.designsystem.icon.AppIcons
import com.letapay.app.core.ui.NavigationItem
import com.letapay.app.feature.chat.ChatRoute
import com.letapay.app.feature.trade.TradeRoute
import com.letapay.app.feature.wallet.WalletRoute
import com.letapay.app.feature.yield.YieldRoute
import org.jetbrains.compose.resources.StringResource

sealed class AuthenticatedNavBarTabItem : NavigationItem {

    data object ChatTab : AuthenticatedNavBarTabItem() {
        override val selectedIcon: ImageVector
            get() = AppIcons.OutlinedDoneAll
        override val icon: ImageVector
            get() = AppIcons.Contact
        override val labelRes: StringResource
            get() = Res.string.chat_tab
        override val contentDescriptionRes: StringResource
            get() = Res.string.chat_tab
        override val graphRoute: String
            get() = ChatRoute.toObjectNavigationRoute()
        override val startDestinationRoute: String
            get() = ChatRoute.toObjectNavigationRoute()
        override val testTag: String
            get() = "ChatTab"
    }

    data object WalletTab : AuthenticatedNavBarTabItem() {
        override val selectedIcon: ImageVector
            get() = AppIcons.FinanceBoarder
        override val icon: ImageVector
            get() = AppIcons.Finance
        override val labelRes: StringResource
            get() = Res.string.wallet_tab
        override val contentDescriptionRes: StringResource
            get() = Res.string.wallet_tab
        override val graphRoute: String
            get() = WalletRoute.toObjectNavigationRoute()
        override val startDestinationRoute: String
            get() = WalletRoute.toObjectNavigationRoute()
        override val testTag: String
            get() = "WalletTab"
    }

    data object TradeTab : AuthenticatedNavBarTabItem() {
        override val selectedIcon: ImageVector
            get() = AppIcons.Payment
        override val icon: ImageVector
            get() = AppIcons.Payment
        override val labelRes: StringResource
            get() = Res.string.trade_tab
        override val contentDescriptionRes: StringResource
            get() = Res.string.trade_tab
        override val graphRoute: String
            get() = TradeRoute.toObjectNavigationRoute()
        override val startDestinationRoute: String
            get() = TradeRoute.toObjectNavigationRoute()
        override val testTag: String
            get() = "TradeTab"
    }

    data object YieldTab : AuthenticatedNavBarTabItem() {
        override val selectedIcon: ImageVector
            get() = AppIcons.Bank
        override val icon: ImageVector
            get() = AppIcons.Bank
        override val labelRes: StringResource
            get() = Res.string.yield_tab
        override val contentDescriptionRes: StringResource
            get() = Res.string.yield_tab
        override val graphRoute: String
            get() = YieldRoute.toObjectNavigationRoute()
        override val startDestinationRoute: String
            get() = YieldRoute.toObjectNavigationRoute()
        override val testTag: String
            get() = "YieldTab"
    }
}
