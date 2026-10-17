package com.neteinstein.networth.feature.manage.ui.components

import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.feature.manage.generated.resources.Res
import com.neteinstein.networth.feature.manage.generated.resources.manage_type_current
import com.neteinstein.networth.feature.manage.generated.resources.manage_type_investment
import com.neteinstein.networth.feature.manage.generated.resources.manage_type_loan
import com.neteinstein.networth.feature.manage.generated.resources.manage_type_mortgage
import com.neteinstein.networth.feature.manage.generated.resources.manage_type_savings
import com.neteinstein.networth.feature.manage.generated.resources.manage_type_term_deposit
import org.jetbrains.compose.resources.StringResource

internal val AccountType.label: StringResource
    get() = when (this) {
        AccountType.CURRENT -> Res.string.manage_type_current
        AccountType.SAVINGS -> Res.string.manage_type_savings
        AccountType.TERM_DEPOSIT -> Res.string.manage_type_term_deposit
        AccountType.INVESTMENT -> Res.string.manage_type_investment
        AccountType.MORTGAGE -> Res.string.manage_type_mortgage
        AccountType.LOAN -> Res.string.manage_type_loan
    }
