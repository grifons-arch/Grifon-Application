package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.PrestaLanguage
import com.example.grifon.core.ShopConfig
import com.example.grifon.core.UiState
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.catalog.CheckoutAddressDto
import com.example.grifon.data.catalog.CheckoutMethodDto
import com.example.grifon.data.catalog.CheckoutOrderItemDto
import com.example.grifon.data.catalog.CheckoutOrderRequestDto
import com.example.grifon.data.catalog.CheckoutSessionDto
import com.example.grifon.data.local.LocalPriceAccessService
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.domain.model.CartItem
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCartUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val getActiveShopUseCase: GetActiveShopUseCase,
    private val getCartUseCase: GetCartUseCase,
    private val shopPreferences: ShopPreferences,
    private val localPriceAccessService: LocalPriceAccessService,
    private val catalogApi: CatalogApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<CheckoutState>>(UiState.Loading)
    val uiState: StateFlow<UiState<CheckoutState>> = _uiState

    private val baseState = MutableStateFlow<BaseCheckoutState?>(null)
    private val draftAddress = MutableStateFlow(CheckoutAddress())
    private val selectedPaymentCode = MutableStateFlow<String?>(null)
    private val selectedShippingCode = MutableStateFlow<String?>(null)
    private val confirmation = MutableStateFlow<CheckoutConfirmation?>(null)

    init {
        observeCheckoutDraft()
        observeCheckoutBase()
        observeUiState()
    }

    fun onRecipientChange(value: String) {
        draftAddress.value = draftAddress.value.copy(recipient = value)
        persistDraft { setCheckoutDraft(recipient = value) }
    }

    fun onPhoneChange(value: String) {
        draftAddress.value = draftAddress.value.copy(phone = value)
        persistDraft { setCheckoutDraft(phone = value) }
    }

    fun onCompanyChange(value: String) {
        draftAddress.value = draftAddress.value.copy(company = value)
        persistDraft { setCheckoutDraft(company = value) }
    }

    fun onStreetChange(value: String) {
        draftAddress.value = draftAddress.value.copy(street = value)
        persistDraft { setCheckoutDraft(street = value) }
    }

    fun onCityChange(value: String) {
        draftAddress.value = draftAddress.value.copy(city = value)
        persistDraft { setCheckoutDraft(city = value) }
    }

    fun onPostalCodeChange(value: String) {
        draftAddress.value = draftAddress.value.copy(postalCode = value)
        persistDraft { setCheckoutDraft(postalCode = value) }
    }

    fun onCountryChange(value: String) {
        draftAddress.value = draftAddress.value.copy(country = value)
        persistDraft { setCheckoutDraft(country = value) }
    }

    fun onPaymentMethodSelected(code: String) {
        selectedPaymentCode.value = code
    }

    fun onShippingMethodSelected(code: String) {
        selectedShippingCode.value = code
    }

    fun dismissConfirmation() {
        confirmation.value = null
    }

    fun confirmCheckout() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (!current.isReadyToConfirm) {
            return
        }
        viewModelScope.launch {
            runCatching {
                catalogApi.createCheckoutOrder(
                    CheckoutOrderRequestDto(
                        shopId = current.shopId.toInt(),
                        customerId = current.customerId ?: error("Missing customer id"),
                        paymentMethodCode = current.selectedPaymentCode ?: error("Missing payment method"),
                        shippingMethodCode = current.selectedShippingCode ?: error("Missing shipping method"),
                        address = current.address.toDto(),
                        items = current.items.map { item ->
                            CheckoutOrderItemDto(
                                productId = item.productId.toIntOrNull() ?: 0,
                                title = item.title,
                                qty = item.qty,
                                unitPrice = item.priceSnapshot,
                                currency = item.currency,
                            )
                        },
                    )
                )
            }.onSuccess { response ->
                confirmation.value = CheckoutConfirmation(
                    reference = response.orderReference,
                    message = response.paymentSessionMessage,
                )
            }.onFailure { error ->
                confirmation.value = CheckoutConfirmation(
                    reference = "ORDER-ERROR",
                    message = error.toCheckoutErrorMessage(),
                )
            }
        }
    }

    private fun observeCheckoutDraft() {
        val identityFlow = combine(
            shopPreferences.currentCustomerFirstName,
            shopPreferences.currentCustomerLastName,
            shopPreferences.currentCustomerEmail,
            shopPreferences.currentCustomerCompany,
        ) { firstName, lastName, email, customerCompany ->
            CheckoutIdentity(
                firstName = firstName,
                lastName = lastName,
                email = email,
                company = customerCompany,
            )
        }

        val contactDraftFlow = combine(
            shopPreferences.checkoutRecipient,
            shopPreferences.checkoutPhone,
            shopPreferences.checkoutCompany,
            shopPreferences.checkoutStreet,
        ) { recipient, phone, company, street ->
            CheckoutContactDraft(
                recipient = recipient,
                phone = phone,
                company = company,
                street = street,
            )
        }

        val locationDraftFlow = combine(
            shopPreferences.checkoutCity,
            shopPreferences.checkoutPostalCode,
            shopPreferences.checkoutCountry,
        ) { city, postalCode, country ->
            CheckoutLocationDraft(
                city = city,
                postalCode = postalCode,
                country = country,
            )
        }

        val draftFlow = combine(contactDraftFlow, locationDraftFlow) { contact, location ->
            CheckoutDraftValues(
                recipient = contact.recipient,
                phone = contact.phone,
                company = contact.company,
                street = contact.street,
                city = location.city,
                postalCode = location.postalCode,
                country = location.country,
            )
        }

        combine(identityFlow, draftFlow) { identity, draft ->
            val fallbackRecipient = listOfNotNull(identity.firstName, identity.lastName)
                .joinToString(" ")
                .trim()
            CheckoutAddress(
                recipient = draft.recipient?.takeIf { it.isNotBlank() } ?: fallbackRecipient,
                email = identity.email.orEmpty(),
                phone = draft.phone.orEmpty(),
                company = draft.company?.takeIf { it.isNotBlank() } ?: identity.company.orEmpty(),
                street = draft.street.orEmpty(),
                city = draft.city.orEmpty(),
                postalCode = draft.postalCode.orEmpty(),
                country = draft.country.orEmpty(),
            )
        }.onEach { draftAddress.value = it }
            .launchIn(viewModelScope)
    }

    private fun observeCheckoutBase() {
        combine(
            getActiveShopUseCase(),
            shopPreferences.currentCustomerId,
            shopPreferences.canViewPrices,
            shopPreferences.appLanguage,
        ) { shopId, customerId, canViewPrices, languageCode ->
            CheckoutSessionState(
                shopId = ShopConfig.normalizeShopId(shopId),
                customerId = customerId,
                canViewPrices = canViewPrices,
                languageCode = languageCode,
            )
        }.flatMapLatest { session ->
            localPriceAccessService.observeCanDisplayPrices(
                shopId = session.shopId,
                customerId = session.customerId,
                canViewPrices = session.canViewPrices,
            ).flatMapLatest { localCanViewPrices ->
                getCartUseCase(session.shopId).onEach { items ->
                    baseState.value = buildBaseState(
                        session = session,
                        items = items,
                        localCanViewPrices = localCanViewPrices,
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun observeUiState() {
        combine(
            baseState.filterNotNull(),
            draftAddress,
            selectedPaymentCode,
            selectedShippingCode,
            confirmation,
        ) { base, address, paymentCode, shippingCode, orderConfirmation ->
            val resolvedPaymentCode = paymentCode
                ?.takeIf { selected -> base.paymentMethods.any { it.code == selected && it.available } }
                ?: base.paymentMethods.firstOrNull { it.available }?.code

            val resolvedShippingCode = shippingCode
                ?.takeIf { selected -> base.shippingMethods.any { it.code == selected && it.available } }
                ?: base.shippingMethods.firstOrNull { it.available }?.code

            if (resolvedPaymentCode != paymentCode) {
                selectedPaymentCode.value = resolvedPaymentCode
            }
            if (resolvedShippingCode != shippingCode) {
                selectedShippingCode.value = resolvedShippingCode
            }

            UiState.Success(
                CheckoutState(
                    shopId = base.shopId,
                    customerId = base.customerId,
                    items = base.items,
                    total = base.total,
                    currency = base.currency,
                    isLoggedIn = base.isLoggedIn,
                    canViewPrices = base.canViewPrices,
                    canCheckout = base.canCheckout,
                    sessionVerified = base.sessionVerified,
                    paymentMethods = base.paymentMethods,
                    shippingMethods = base.shippingMethods,
                    selectedPaymentCode = resolvedPaymentCode,
                    selectedShippingCode = resolvedShippingCode,
                    address = address.withDefaultCountryIfMissing(base.shopId),
                    notes = base.notes,
                    confirmation = orderConfirmation,
                )
            )
        }.onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    private suspend fun buildBaseState(
        session: CheckoutSessionState,
        items: List<CartItem>,
        localCanViewPrices: Boolean,
    ): BaseCheckoutState {
        return try {
            val remoteSession = catalogApi.getCheckoutSession(
                shopId = session.shopId.toInt(),
                lang = PrestaLanguage.toLangId(session.languageCode),
                customerId = session.customerId,
            )
            remoteSession.toBaseState(items)
        } catch (_: Exception) {
            buildFallbackBaseState(session, items, localCanViewPrices)
        }
    }

    private fun buildFallbackBaseState(
        session: CheckoutSessionState,
        items: List<CartItem>,
        localCanViewPrices: Boolean,
    ): BaseCheckoutState {
        val canCheckout = session.customerId != null && localCanViewPrices
        val notes = when {
            session.customerId == null ->
                listOf("Login is required before checkout can continue inside the app.")
            !localCanViewPrices ->
                listOf("Wholesale approval is required before prices and ordering become available.")
            else ->
                listOf("Live checkout verification from the gateway is not available yet for this session.")
        }

        return BaseCheckoutState(
            shopId = session.shopId,
            customerId = session.customerId,
            items = items,
            total = items.sumOf { it.qty * it.priceSnapshot }.takeIf { localCanViewPrices },
            currency = items.firstOrNull()?.currency,
            isLoggedIn = session.customerId != null,
            canViewPrices = localCanViewPrices,
            canCheckout = false,
            sessionVerified = false,
            paymentMethods = listOf(
                CheckoutMethod(code = "paypal", title = "PayPal", available = true),
                CheckoutMethod(code = "stripe_cards", title = "Cards via Stripe", available = true),
            ),
            shippingMethods = listOf(
                CheckoutMethod(
                    code = "prestashippingquote",
                    title = "Shipping quote and carrier selection",
                    available = true,
                ),
            ),
            notes = notes,
        )
    }

    private fun persistDraft(block: suspend ShopPreferences.() -> Unit) {
        viewModelScope.launch {
            shopPreferences.block()
        }
    }
}

data class CheckoutState(
    val shopId: String,
    val customerId: Int?,
    val items: List<CartItem>,
    val total: Double?,
    val currency: String?,
    val isLoggedIn: Boolean,
    val canViewPrices: Boolean,
    val canCheckout: Boolean,
    val sessionVerified: Boolean,
    val paymentMethods: List<CheckoutMethod>,
    val shippingMethods: List<CheckoutMethod>,
    val selectedPaymentCode: String?,
    val selectedShippingCode: String?,
    val address: CheckoutAddress,
    val notes: List<String>,
    val confirmation: CheckoutConfirmation?,
) {
    val isReadyToConfirm: Boolean
        get() = sessionVerified &&
            canCheckout &&
            items.isNotEmpty() &&
            selectedPaymentCode != null &&
            selectedShippingCode != null &&
            address.isComplete
}

data class CheckoutMethod(
    val code: String,
    val title: String,
    val available: Boolean,
)

data class CheckoutAddress(
    val recipient: String = "",
    val email: String = "",
    val phone: String = "",
    val company: String = "",
    val street: String = "",
    val city: String = "",
    val postalCode: String = "",
    val country: String = "",
) {
    val isComplete: Boolean
        get() = recipient.isNotBlank() &&
            email.isNotBlank() &&
            phone.isNotBlank() &&
            street.isNotBlank() &&
            city.isNotBlank() &&
            postalCode.isNotBlank() &&
            country.isNotBlank()
}

data class CheckoutConfirmation(
    val reference: String,
    val message: String,
)

private data class BaseCheckoutState(
    val shopId: String,
    val customerId: Int?,
    val items: List<CartItem>,
    val total: Double?,
    val currency: String?,
    val isLoggedIn: Boolean,
    val canViewPrices: Boolean,
    val canCheckout: Boolean,
    val sessionVerified: Boolean,
    val paymentMethods: List<CheckoutMethod>,
    val shippingMethods: List<CheckoutMethod>,
    val notes: List<String>,
)

private data class CheckoutSessionState(
    val shopId: String,
    val customerId: Int?,
    val canViewPrices: Boolean,
    val languageCode: String,
)

private data class CheckoutIdentity(
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val company: String?,
)

private data class CheckoutDraftValues(
    val recipient: String?,
    val phone: String?,
    val company: String?,
    val street: String?,
    val city: String?,
    val postalCode: String?,
    val country: String?,
)

private data class CheckoutContactDraft(
    val recipient: String?,
    val phone: String?,
    val company: String?,
    val street: String?,
)

private data class CheckoutLocationDraft(
    val city: String?,
    val postalCode: String?,
    val country: String?,
)

private fun CheckoutSessionDto.toBaseState(items: List<CartItem>): BaseCheckoutState {
    return BaseCheckoutState(
        shopId = shopId.toString(),
        customerId = customerId,
        items = items,
        total = items.sumOf { it.qty * it.priceSnapshot }.takeIf { canViewPrices },
        currency = items.firstOrNull()?.currency,
        isLoggedIn = isLoggedIn,
        canViewPrices = canViewPrices,
        canCheckout = canCheckout,
        sessionVerified = true,
        paymentMethods = paymentMethods.map { it.toState() },
        shippingMethods = shippingMethods.map { it.toState() },
        notes = notes,
    )
}

private fun CheckoutMethodDto.toState(): CheckoutMethod {
    return CheckoutMethod(
        code = code,
        title = title,
        available = available,
    )
}

private fun CheckoutAddress.withDefaultCountryIfMissing(shopId: String): CheckoutAddress {
    if (country.isNotBlank()) return this
    return copy(
        country = if (ShopConfig.normalizeShopId(shopId) == "1") "Sweden" else "Greece"
    )
}

private fun CheckoutAddress.toDto(): CheckoutAddressDto {
    return CheckoutAddressDto(
        recipient = recipient,
        email = email,
        phone = phone,
        company = company.takeIf { it.isNotBlank() },
        street = street,
        city = city,
        postalCode = postalCode,
        country = country,
    )
}

private fun Throwable.toCheckoutErrorMessage(): String {
    if (this is HttpException) {
        val body = response()?.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            runCatching {
                val json = JSONObject(body)
                val message = json.optString("message").trim()
                val code = json.optString("code").trim()
                when {
                    message.isNotEmpty() -> message
                    code.isNotEmpty() -> code
                    else -> "Checkout order creation failed."
                }
            }.getOrNull()?.let { return it }
        }
        return "Checkout request failed with HTTP ${code()}."
    }
    return message ?: "Checkout order creation failed."
}
