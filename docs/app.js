import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.5/firebase-app.js";
import {
  getAuth,
  signInAnonymously,
  setPersistence,
  browserLocalPersistence
} from "https://www.gstatic.com/firebasejs/10.12.5/firebase-auth.js";
import {
  getFirestore,
  arrayUnion,
  collection,
  deleteDoc,
  doc,
  documentId,
  getDoc,
  getDocs,
  onSnapshot,
  orderBy,
  query,
  setDoc,
  updateDoc
} from "https://www.gstatic.com/firebasejs/10.12.5/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyC009KuC5j50FGfKa_gm7PgyFaQo-1x9zU",
  authDomain: "finanzasfamiliares-228d2.firebaseapp.com",
  projectId: "finanzasfamiliares-228d2",
  storageBucket: "finanzasfamiliares-228d2.firebasestorage.app",
  messagingSenderId: "362613753184"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

const FAMILY_STORAGE_KEY = "finanzas_web_active_family_id";
const THEME_STORAGE_KEY = "finanzas_web_theme_mode";
const ACCENT_STORAGE_KEY = "finanzas_web_accent_color";
const JOIN_CODES_COLLECTION = "familyJoinCodes";
const INCOME_CURRENCY = {
  USD: "USD",
  UYU: "UYU"
};
const CARD_KIND = {
  PUNCTUAL: "PUNCTUAL",
  RECURRING: "RECURRING",
  INSTALLMENT: "INSTALLMENT"
};
const THEME_MODES = ["system", "light", "dark"];
const ACCENT_COLORS = ["green", "blue", "teal", "indigo", "violet", "slate"];

const defaultCategories = [
  "Comida",
  "Ropa",
  "Transporte",
  "Entretenimiento",
  "Servicios Publicos",
  "Salud",
  "Streaming",
  "Educacion",
  "Higiene",
  "Belleza",
  "Reparaciones",
  "Hogar",
  "Alquiler",
  "Impuestos",
  "Mascotas"
];

const defaultConfig = {
  incomeCurrency: INCOME_CURRENCY.USD,
  defaultExchangeRate: 0,
  defaultCardExchangeOffset: 0,
  marginGreenThresholdPct: 20000,
  marginYellowThresholdPct: 5000,
  expenseCategories: defaultCategories,
  planningThroughYearMonth: "",
  saving1Name: "Ahorro 1",
  saving2Name: "Ahorro 2",
  saving3Name: "Ahorro 3"
};

const state = {
  user: null,
  family: null,
  config: { ...defaultConfig },
  month: null,
  previousMonth: null,
  availableMonths: [],
  yearMonth: currentYearMonth(),
  route: routeFromHash(),
  monthUnsubscribe: null,
  familyUnsubscribe: null,
  configUnsubscribe: null,
  monthsUnsubscribe: null,
  isBooting: true,
  fatalError: null,
  isDeletingMonths: false,
  appearance: loadAppearance()
};

const appShell = document.querySelector("#app");
const screen = document.querySelector("#screen");
const modal = document.querySelector("#modal");
const toast = document.querySelector("#toast");
const familyChip = document.querySelector("#family-chip");
const monthPicker = document.querySelector("#month-picker");
const monthPrev = document.querySelector("#month-prev");
const monthNext = document.querySelector("#month-next");
const currentMonthButton = document.querySelector("#current-month");
const generateMonthButton = document.querySelector("#generate-month");
const deleteMonthsButton = document.querySelector("#delete-months");

bindChrome();
applyAppearance();
boot();

window.matchMedia?.("(prefers-color-scheme: dark)")?.addEventListener("change", () => {
  if (state.appearance.themeMode === "system") applyAppearance();
});

async function boot() {
  try {
    await setPersistence(auth, browserLocalPersistence);
    if (!auth.currentUser) {
      await signInAnonymously(auth);
    }
    state.user = auth.currentUser;
    await restoreFamily();
  } catch (error) {
    state.fatalError = error;
    renderFatalError(error);
  } finally {
    state.isBooting = false;
    render();
  }
}

async function restoreFamily() {
  const rememberedFamilyId = localStorage.getItem(FAMILY_STORAGE_KEY);
  if (rememberedFamilyId) {
    const family = await tryReadFamily(rememberedFamilyId);
    if (family) {
      activateFamily(family);
      return;
    }
    localStorage.removeItem(FAMILY_STORAGE_KEY);
  }

  const ownFamily = await tryReadFamily(state.user.uid);
  if (ownFamily) {
    activateFamily(ownFamily);
  }
}

async function tryReadFamily(familyId) {
  try {
    const snap = await getDoc(familyRef(familyId));
    return snap.exists() ? snap.data() : null;
  } catch {
    return null;
  }
}

function activateFamily(family) {
  state.family = family;
  localStorage.setItem(FAMILY_STORAGE_KEY, family.id);
  subscribeFamilyData();
}

function subscribeFamilyData() {
  cleanupSubscriptions();
  state.familyUnsubscribe = onSnapshot(familyRef(state.family.id), (snap) => {
    if (snap.exists()) {
      state.family = snap.data();
      renderChrome();
      if (state.route === "config") render();
    }
  });
  state.configUnsubscribe = onSnapshot(configRef(), async (snap) => {
    if (snap.exists()) {
      state.config = normalizeConfig(snap.data());
      render();
    } else {
      await setDoc(configRef(), { ...defaultConfig });
    }
  });
  state.monthsUnsubscribe = onSnapshot(
    query(monthsCollectionRef(), orderBy(documentId())),
    (snap) => {
      state.availableMonths = snap.docs.map((item) => item.id).sort();
      renderChrome();
      if (state.route === "config") render();
    }
  );
  subscribeMonth(state.yearMonth);
}

function cleanupSubscriptions() {
  [state.familyUnsubscribe, state.configUnsubscribe, state.monthsUnsubscribe, state.monthUnsubscribe]
    .forEach((unsubscribe) => unsubscribe?.());
  state.familyUnsubscribe = null;
  state.configUnsubscribe = null;
  state.monthsUnsubscribe = null;
  state.monthUnsubscribe = null;
}

function subscribeMonth(yearMonth) {
  state.monthUnsubscribe?.();
  state.month = null;
  state.previousMonth = null;
  state.monthUnsubscribe = onSnapshot(monthRef(yearMonth), async (snap) => {
    if (snap.exists()) {
      state.month = normalizeMonth(snap.data(), yearMonth);
      loadPreviousMonth(yearMonth);
      render();
    } else {
      await ensureMonthDocument(yearMonth);
    }
  });
  renderChrome();
}

async function loadPreviousMonth(yearMonth) {
  const previousKey = addMonths(yearMonth, -1);
  try {
    const snap = await getDoc(monthRef(previousKey));
    state.previousMonth = snap.exists() ? normalizeMonth(snap.data(), previousKey) : null;
    if (state.route === "analysis") render();
  } catch {
    state.previousMonth = null;
  }
}

function bindChrome() {
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.addEventListener("click", () => {
      location.hash = button.dataset.route;
    });
  });
  window.addEventListener("hashchange", () => {
    state.route = routeFromHash();
    render();
  });
  monthPrev.addEventListener("click", () => goToNearestMonth(-1));
  monthNext.addEventListener("click", () => goToNearestMonth(1));
  currentMonthButton.addEventListener("click", () => setMonth(currentYearMonth()));
  generateMonthButton.addEventListener("click", openGenerateMonthsDialog);
  deleteMonthsButton.addEventListener("click", openDeleteMonthsDialog);
  monthPicker.addEventListener("click", openMonthPicker);
}

function loadAppearance() {
  const themeMode = localStorage.getItem(THEME_STORAGE_KEY);
  const accentColor = localStorage.getItem(ACCENT_STORAGE_KEY);
  return {
    themeMode: THEME_MODES.includes(themeMode) ? themeMode : "system",
    accentColor: ACCENT_COLORS.includes(accentColor) ? accentColor : "green"
  };
}

function applyAppearance() {
  const prefersDark = window.matchMedia?.("(prefers-color-scheme: dark)")?.matches;
  const resolvedTheme = state.appearance.themeMode === "system"
    ? (prefersDark ? "dark" : "light")
    : state.appearance.themeMode;
  document.documentElement.dataset.theme = resolvedTheme;
  document.documentElement.dataset.accent = state.appearance.accentColor;
}

function updateThemeMode(themeMode) {
  if (!THEME_MODES.includes(themeMode)) return;
  state.appearance.themeMode = themeMode;
  localStorage.setItem(THEME_STORAGE_KEY, themeMode);
  applyAppearance();
  render();
}

function updateAccentColor(accentColor) {
  if (!ACCENT_COLORS.includes(accentColor)) return;
  state.appearance.accentColor = accentColor;
  localStorage.setItem(ACCENT_STORAGE_KEY, accentColor);
  applyAppearance();
  render();
}

function routeFromHash() {
  return (location.hash || "#summary").replace("#", "") || "summary";
}

function render() {
  renderChrome();
  if (state.isBooting) {
    screen.innerHTML = loadingPanel("Cargando datos...");
    return;
  }
  if (state.fatalError) {
    renderFatalError(state.fatalError);
    return;
  }
  if (!state.family) {
    renderSetup();
    return;
  }
  if (!state.month && state.route !== "config") {
    screen.innerHTML = loadingPanel("Preparando mes...");
    return;
  }

  const routes = {
    summary: renderSummary,
    expenses: renderExpenses,
    donations: renderDonations,
    savings: renderSavings,
    analysis: renderAnalysis,
    config: renderConfig
  };
  (routes[state.route] || renderSummary)();
}

function renderChrome() {
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.classList.toggle("active", button.dataset.route === state.route);
    button.disabled = !state.family;
  });
  monthPicker.textContent = formatMonthLabel(state.yearMonth);
  familyChip.classList.remove("skeleton");
  familyChip.textContent = state.family
    ? `Familia ${state.family.joinCode || state.family.id.slice(0, 6)}`
    : state.isBooting
      ? "Conectando..."
      : "Sin familia activa";
  const hasFamily = Boolean(state.family);
  monthPrev.disabled = !hasFamily;
  monthPicker.disabled = !hasFamily;
  monthNext.disabled = !hasFamily;
  currentMonthButton.disabled = !hasFamily;
  generateMonthButton.disabled = !hasFamily;
  deleteMonthsButton.disabled = !hasFamily || state.isDeletingMonths;
}

function renderSetup() {
  appShell.classList.add("setup-mode");
  screen.innerHTML = `
    <div class="setup-wrap">
      <section class="setup-panel">
        <p class="eyebrow">Finanzas Familiares Web</p>
        <h2>Conectar la computadora</h2>
        <p class="muted">Usa el codigo de familia de Android para ver la misma base de datos, o crea una familia nueva para probar la web.</p>
        <div class="setup-options">
          <article class="setup-option">
            <h3>Unirse a familia</h3>
            <form id="join-family-form" class="form-grid">
              <div class="field">
                <label for="join-code">Codigo</label>
                <input id="join-code" class="input" inputmode="numeric" maxlength="6" required>
              </div>
              <button class="primary-button" type="submit">Entrar</button>
            </form>
          </article>
          <article class="setup-option">
            <h3>Crear familia</h3>
            <form id="create-family-form" class="form-grid">
              <div class="inline-fields">
                <div class="field">
                  <label for="setup-currency">Ingreso</label>
                  <select id="setup-currency" class="select">
                    <option value="USD">USD</option>
                    <option value="UYU">UYU</option>
                  </select>
                </div>
                <div class="field">
                  <label for="setup-purchase">Compra</label>
                  <input id="setup-purchase" class="input" inputmode="decimal" placeholder="40">
                </div>
                <div class="field">
                  <label for="setup-sale">Tarjeta</label>
                  <input id="setup-sale" class="input" inputmode="decimal" placeholder="42">
                </div>
              </div>
              <div class="inline-fields">
                <div class="field">
                  <label for="setup-green">Verde desde</label>
                  <input id="setup-green" class="input" inputmode="decimal" value="20000">
                </div>
                <div class="field">
                  <label for="setup-yellow">Amarillo desde</label>
                  <input id="setup-yellow" class="input" inputmode="decimal" value="5000">
                </div>
              </div>
              <button class="secondary-button" type="submit">Crear</button>
            </form>
          </article>
        </div>
      </section>
    </div>
  `;
  document.querySelector("#join-family-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const code = document.querySelector("#join-code").value.trim();
    await withToastError(async () => {
      await joinFamily(code);
      toastMessage("Familia conectada");
    });
  });
  document.querySelector("#create-family-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    await withToastError(async () => {
      await createFamily(readSetupConfig());
      toastMessage("Familia creada");
    });
  });
}

function renderSummary() {
  appShell.classList.remove("setup-mode");
  const month = state.month;
  const config = state.config;
  const primaryIncomeAmount = primaryIncomeAmountFor(month, config.incomeCurrency);
  const primaryIncomeUYU = primaryIncomeInUYU(month, config.incomeCurrency);
  const totalIncome = totalIncomeInUYU(month, config.incomeCurrency);
  const obligations = totalObligationsInUYU(month, config.incomeCurrency);
  const available = availableBalanceInUYU(month, config.incomeCurrency);
  const pending = pendingObligationsInUYU(month, config.incomeCurrency);
  const margin = availableMarginInUYU(month, config.incomeCurrency);
  const zoneClass = marginColorClass(margin, config);

  screen.innerHTML = `
    <div class="screen-grid two-col">
      <section class="hero-panel ${zoneClass}">
        <div class="hero-header">
          <div>
            <span class="metric-label">Margen disponible</span>
            <div class="hero-value">${formatUYU(margin)}</div>
            <p class="hero-subtitle">${month.availableBalanceOverrideUYU == null ? "Disponible actual menos obligaciones restantes" : "Disponible actual ajustado manualmente"}</p>
          </div>
          <button class="icon-button" type="button" data-action="edit-balance" title="Editar disponible">✎</button>
        </div>
        <div class="hero-metrics">
          ${metricPill("Disponible actual", formatUYU(available))}
          ${metricPill("Obligaciones restantes", formatUYU(pending))}
        </div>
      </section>

      <section class="finance-card">
        ${sectionTitle("Ingreso", "I", `<button class="text-button" data-action="edit-income" type="button">Editar</button>`)}
        <div class="metric-grid">
          ${metricPill("Principal", config.incomeCurrency === "USD" ? formatUSD(primaryIncomeAmount) : formatUYU(primaryIncomeAmount))}
          ${metricPill("Principal en UYU", formatUYU(primaryIncomeUYU))}
          ${metricPill("Variables", formatUYU(variableIncomeUYU(month)))}
          ${metricPill("Total ingresos", formatUYU(totalIncome))}
        </div>
      </section>

      <section class="finance-card">
        ${sectionTitle("Tipo de cambio", "$", `<button class="text-button" data-action="edit-rates" type="button">Editar</button>`)}
        <div class="metric-grid">
          ${metricPill("Compra", formatUYU(month.exchangeRate))}
          ${metricPill("Tarjeta", formatUYU(cardExchangeRate(month)))}
        </div>
      </section>

      <section class="finance-card">
        ${sectionTitle("Obligaciones", "O", "")}
        <div class="metric-grid">
          ${metricPill("Donativos", formatUYU(donationsInUYU(month, config.incomeCurrency)))}
          ${metricPill("Gastos fijos", formatUYU(sumEntries(month.fixedExpenses, cardExchangeRate(month))))}
          ${metricPill("Variables", formatUYU(sumEntries(month.variableExpenses, cardExchangeRate(month))))}
          ${metricPill("Tarjeta + deudas", formatUYU(sumEntries(month.cardExpenses, cardExchangeRate(month)) + sumEntries(month.debts, cardExchangeRate(month))))}
        </div>
      </section>

      <section class="finance-card">
        ${sectionTitle("Ingresos variables", "+", `<button class="text-button" data-action="add-variable-income" type="button">Agregar</button>`)}
        <div class="item-list">
          ${renderMoneyList(month.variableIncomes, "variableIncomes", month.exchangeRate)}
        </div>
      </section>
    </div>
  `;

  screen.querySelector('[data-action="edit-balance"]').addEventListener("click", openBalanceDialog);
  screen.querySelector('[data-action="edit-income"]').addEventListener("click", openIncomeDialog);
  screen.querySelector('[data-action="edit-rates"]').addEventListener("click", openRatesDialog);
  screen.querySelector('[data-action="add-variable-income"]').addEventListener("click", () => openVariableIncomeDialog());
  bindListActions();
}

function renderExpenses() {
  const month = state.month;
  const search = "";
  const cardRate = cardExchangeRate(month);
  const sections = [
    ["fixedExpenses", "Fijos", "F", month.fixedExpenses],
    ["variableExpenses", "Variables", "V", month.variableExpenses],
    ["cardExpenses", "Tarjeta", "T", month.cardExpenses],
    ["debts", "Deudas", "D", month.debts]
  ];

  screen.innerHTML = `
    <div class="toolbar">
      <div class="toolbar-left">
        <input id="expense-search" class="input" type="search" placeholder="Buscar" style="width: min(320px, 100%);">
        <button class="filter-chip active" data-filter="all" type="button">Todos</button>
        <button class="filter-chip" data-filter="pending" type="button">Pendientes</button>
        <button class="filter-chip" data-filter="paid" type="button">Pagados</button>
      </div>
    </div>
    <div id="expense-sections" class="screen-grid">
      ${sections.map(([field, label, icon, items]) => renderExpenseSection(field, label, icon, items, cardRate, search, "all")).join("")}
    </div>
  `;

  const searchInput = screen.querySelector("#expense-search");
  const filterButtons = [...screen.querySelectorAll("[data-filter]")];
  const rerenderSections = () => {
    const filter = filterButtons.find((button) => button.classList.contains("active"))?.dataset.filter || "all";
    const queryText = searchInput.value.trim();
    screen.querySelector("#expense-sections").innerHTML = sections
      .map(([field, label, icon, items]) => renderExpenseSection(field, label, icon, items, cardRate, queryText, filter))
      .join("");
    bindListActions();
    bindExpenseButtons();
  };
  searchInput.addEventListener("input", rerenderSections);
  filterButtons.forEach((button) => {
    button.addEventListener("click", () => {
      filterButtons.forEach((item) => item.classList.remove("active"));
      button.classList.add("active");
      rerenderSections();
    });
  });
  bindExpenseButtons();
  bindListActions();
}

function renderExpenseSection(field, label, icon, items, rate, queryText, filter) {
  const filtered = items.filter((item) => {
    const haystack = `${item.name || ""} ${item.category || ""}`.toLowerCase();
    const matchesSearch = haystack.includes(queryText.toLowerCase());
    const isPaidItem = isPaid(item);
    const matchesFilter = filter === "all" || (filter === "paid" && isPaidItem) || (filter === "pending" && !isPaidItem);
    return matchesSearch && matchesFilter;
  });
  const total = filtered.reduce((sum, item) => sum + totalEntryUYU(item, rate), 0);
  return `
    <section class="finance-card">
      ${sectionTitle(`${label} · ${formatUYU(total)}`, icon, `<button class="text-button" data-add-expense="${field}" type="button">Agregar</button>`)}
      <div class="item-list">
        ${renderMoneyList(filtered, field, rate)}
      </div>
    </section>
  `;
}

function bindExpenseButtons() {
  screen.querySelectorAll("[data-add-expense]").forEach((button) => {
    button.addEventListener("click", () => openExpenseDialog(button.dataset.addExpense));
  });
}

function renderDonations() {
  const month = state.month;
  const total = donationsInUYU(month, state.config.incomeCurrency);
  screen.innerHTML = `
    <div class="screen-grid">
      <section class="finance-card">
        ${sectionTitle("Donativos", "D", `<button class="text-button" data-action="add-donation" type="button">Agregar</button>`)}
        <div class="metric-grid">
          ${metricPill("Total", formatUYU(total))}
          ${metricPill("Pendiente", formatUYU(month.donations.filter((item) => !isPaid(item)).reduce((sum, item) => sum + donationTotalUYU(item, primaryIncomeInUYU(month, state.config.incomeCurrency), cardExchangeRate(month)), 0)))}
        </div>
      </section>
      <section class="finance-card">
        <div class="item-list">
          ${renderDonationList(month.donations)}
        </div>
      </section>
    </div>
  `;
  screen.querySelector('[data-action="add-donation"]').addEventListener("click", () => openDonationDialog());
  bindListActions();
}

function renderSavings() {
  const month = state.month;
  const totals = savingsTotals(month);
  screen.innerHTML = `
    <div class="screen-grid two-col">
      <section class="hero-panel blue-zone">
        <div class="hero-header">
          <div>
            <span class="metric-label">Total ahorros</span>
            <div class="savings-total-grid">
              <div class="savings-total-amount">
                <span class="metric-label">Pesos</span>
                <strong>${formatUYU(totals.uyu)}</strong>
              </div>
              <div class="savings-total-amount">
                <span class="metric-label">Dolares</span>
                <strong>${formatUSD(totals.usd)}</strong>
              </div>
            </div>
          </div>
        </div>
      </section>
      <section class="finance-card">
        ${sectionTitle("Ahorros", "A", "")}
        <div class="item-list">
          ${month.savings.map((saving) => renderSavingItem(saving)).join("") || emptyState("Sin ahorros")}
        </div>
      </section>
    </div>
  `;
  bindListActions();
}

function renderAnalysis() {
  const month = state.month;
  const config = state.config;
  const typeItems = [
    ["Donativos", donationsInUYU(month, config.incomeCurrency), "#2e7d32"],
    ["Fijos", sumEntries(month.fixedExpenses, cardExchangeRate(month)), "#1565c0"],
    ["Variables", sumEntries(month.variableExpenses, cardExchangeRate(month)), "#6a1b9a"],
    ["Tarjeta", sumEntries(month.cardExpenses, cardExchangeRate(month)), "#ef6c00"],
    ["Deudas", sumEntries(month.debts, cardExchangeRate(month)), "#c62828"]
  ];
  const categoryItems = categoryTotals(month);
  const previous = state.previousMonth;
  const comparisons = previous ? [
    ["Ingresos", totalIncomeInUYU(month, config.incomeCurrency), totalIncomeInUYU(previous, config.incomeCurrency)],
    ["Obligaciones", totalObligationsInUYU(month, config.incomeCurrency), totalObligationsInUYU(previous, config.incomeCurrency)],
    ["Margen", availableMarginInUYU(month, config.incomeCurrency), availableMarginInUYU(previous, config.incomeCurrency)]
  ] : [];

  screen.innerHTML = `
    <div class="screen-grid two-col">
      <section class="finance-card">
        ${sectionTitle("Por tipo", "B", "")}
        ${barList(typeItems)}
      </section>
      <section class="finance-card">
        ${sectionTitle("Por categoria", "C", "")}
        ${categoryItems.length ? barList(categoryItems) : emptyState("Sin gastos por categoria")}
      </section>
      <section class="finance-card">
        ${sectionTitle("Comparacion mensual", "M", "")}
        ${comparisons.length ? comparisonList(comparisons) : `<p class="muted">No hay mes anterior disponible.</p>`}
      </section>
    </div>
  `;
}

function renderConfig() {
  const config = state.config;
  screen.innerHTML = `
    <div class="screen-grid two-col">
      <section class="finance-card">
        ${sectionTitle("Apariencia", "A", "")}
        <div class="form-grid">
          <div class="field">
            <label>Modo</label>
            <div class="radio-row">
              ${themeChip("system", "Sistema")}
              ${themeChip("light", "Claro")}
              ${themeChip("dark", "Oscuro")}
            </div>
          </div>
          <div class="field">
            <label>Color</label>
            <div class="radio-row">
              ${accentChip("green", "Verde")}
              ${accentChip("blue", "Azul")}
              ${accentChip("teal", "Teal")}
              ${accentChip("indigo", "Indigo")}
              ${accentChip("violet", "Violeta")}
              ${accentChip("slate", "Slate")}
            </div>
          </div>
        </div>
      </section>

      <section class="finance-card">
        ${sectionTitle("Configuracion", "C", "")}
        <form id="config-form" class="form-grid">
          <div class="field">
            <label for="income-currency">Moneda de ingreso</label>
            <select id="income-currency" class="select">
              <option value="USD" ${config.incomeCurrency === "USD" ? "selected" : ""}>USD</option>
              <option value="UYU" ${config.incomeCurrency === "UYU" ? "selected" : ""}>UYU</option>
            </select>
          </div>
          <div class="inline-fields">
            <div class="field">
              <label for="default-rate">Compra</label>
              <input id="default-rate" class="input" inputmode="decimal" value="${escapeAttr(toInput(config.defaultExchangeRate))}">
            </div>
            <div class="field">
              <label for="default-card-rate">Tarjeta</label>
              <input id="default-card-rate" class="input" inputmode="decimal" value="${escapeAttr(toInput(config.defaultExchangeRate + config.defaultCardExchangeOffset))}">
            </div>
          </div>
          <div class="inline-fields">
            <div class="field">
              <label class="label-with-swatch" for="green-threshold"><span class="threshold-swatch green-swatch"></span>Verde desde</label>
              <input id="green-threshold" class="input" inputmode="decimal" value="${escapeAttr(toInput(config.marginGreenThresholdPct))}">
            </div>
            <div class="field">
              <label class="label-with-swatch" for="yellow-threshold"><span class="threshold-swatch yellow-swatch"></span>Amarillo desde</label>
              <input id="yellow-threshold" class="input" inputmode="decimal" value="${escapeAttr(toInput(config.marginYellowThresholdPct))}">
            </div>
          </div>
          <button class="primary-button" type="submit">Guardar</button>
        </form>
      </section>

      <section class="finance-card">
        ${sectionTitle("Familia", "F", "")}
        <div class="metric-grid">
          ${metricPill("Codigo", state.family?.joinCode || "Pendiente")}
          ${metricPill("Sesiones autorizadas", String(state.family?.memberIds?.length || 1))}
        </div>
        <div class="divider"></div>
        <form id="join-other-family-form" class="form-grid">
          <div class="field">
            <label for="other-family-code">Unirse a otra familia</label>
            <input id="other-family-code" class="input" inputmode="numeric" maxlength="6">
          </div>
          <button class="secondary-button" type="submit">Cambiar familia</button>
        </form>
      </section>
    </div>
  `;
  screen.querySelectorAll("[data-theme-mode]").forEach((button) => {
    button.addEventListener("click", () => updateThemeMode(button.dataset.themeMode));
  });
  screen.querySelectorAll("[data-accent-color]").forEach((button) => {
    button.addEventListener("click", () => updateAccentColor(button.dataset.accentColor));
  });
  screen.querySelector("#config-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    await withToastError(async () => {
      const purchase = parseAmount(document.querySelector("#default-rate").value);
      const card = parseAmount(document.querySelector("#default-card-rate").value);
      await setDoc(configRef(), {
        ...config,
        incomeCurrency: document.querySelector("#income-currency").value,
        defaultExchangeRate: purchase,
        defaultCardExchangeOffset: Math.max(0, card - purchase),
        marginGreenThresholdPct: parseAmount(document.querySelector("#green-threshold").value),
        marginYellowThresholdPct: parseAmount(document.querySelector("#yellow-threshold").value)
      });
      toastMessage("Configuracion guardada");
    });
  });
  screen.querySelector("#join-other-family-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const code = document.querySelector("#other-family-code").value.trim();
    await withToastError(async () => {
      await joinFamily(code);
      toastMessage("Familia actualizada");
    });
  });
}

function renderMoneyList(items, type, rate) {
  return items.map((item) => renderMoneyItem(item, type, rate)).join("") || emptyState("Sin movimientos");
}

function renderMoneyItem(item, type, rate) {
  const paid = isPaid(item);
  const supportsPayment = type !== "variableIncomes";
  const meta = [
    item.category,
    isInUSD(item) ? formatUSD(Number(item.amountUSD || 0)) : null,
    item.kind ? labelCardKind(item.kind) : null,
    item.totalInstallments > 1 ? `${item.currentInstallment || 1}/${item.totalInstallments}` : null
  ].filter(Boolean).join(" · ");
  return `
    <article class="money-item ${paid ? "paid" : ""}" data-id="${escapeAttr(item.id)}" data-type="${escapeAttr(type)}">
      ${supportsPayment
        ? `<input type="checkbox" ${paid ? "checked" : ""} data-action="toggle-paid" title="Marcar pagado">`
        : `<span class="soft-badge">+</span>`}
      <div>
        <p class="item-title">${escapeHtml(item.name || "Sin nombre")}</p>
        <p class="item-meta">${escapeHtml(meta || "Sin categoria")}</p>
      </div>
      <div class="item-actions">
        <span class="item-amount">${formatUYU(totalEntryUYU(item, rate))}</span>
        <button class="square-button" type="button" data-action="edit-item" title="Editar">✎</button>
        <button class="square-button danger" type="button" data-action="delete-item" title="Eliminar">×</button>
      </div>
    </article>
  `;
}

function renderDonationList(items) {
  return items.map((item) => {
    const paid = isPaid(item);
    const value = donationTotalUYU(item, primaryIncomeInUYU(state.month, state.config.incomeCurrency), cardExchangeRate(state.month));
    const meta = item.percentOfPrimaryIncome != null
      ? `${toInput(item.percentOfPrimaryIncome)}% del ingreso`
      : isInUSD(item)
        ? formatUSD(Number(item.amountUSD || 0))
        : formatUYU(Number(item.amountUYU || 0));
    return `
      <article class="money-item ${paid ? "paid" : ""}" data-id="${escapeAttr(item.id)}" data-type="donations">
        <input type="checkbox" ${paid ? "checked" : ""} data-action="toggle-paid" title="Marcar pagado">
        <div>
          <p class="item-title">${escapeHtml(item.name || "Donativo")}</p>
          <p class="item-meta">${escapeHtml(meta)}</p>
        </div>
        <div class="item-actions">
          <span class="item-amount">${formatUYU(value)}</span>
          <button class="square-button" type="button" data-action="edit-item" title="Editar">✎</button>
          <button class="square-button danger" type="button" data-action="delete-item" title="Eliminar">×</button>
        </div>
      </article>
    `;
  }).join("") || emptyState("Sin donativos");
}

function renderSavingItem(saving) {
  const amount = saving.currencyCode === "USD" || saving.currency === "USD"
    ? formatUSD(Number(saving.amountUSD || 0))
    : formatUYU(Number(saving.amountUYU || 0));
  return `
    <article class="money-item" data-id="${escapeAttr(saving.id)}" data-type="savings">
      <span class="soft-badge">A</span>
      <div>
        <p class="item-title">${escapeHtml(saving.name || "Ahorro")}</p>
        <p class="item-meta">${escapeHtml(saving.currency || "UYU")}</p>
      </div>
      <div class="item-actions">
        <span class="item-amount">${amount}</span>
        <button class="square-button" type="button" data-action="adjust-saving" title="Ajustar">✎</button>
      </div>
    </article>
  `;
}

function bindListActions() {
  screen.querySelectorAll('[data-action="toggle-paid"]').forEach((input) => {
    input.addEventListener("change", async () => {
      const itemElement = input.closest("[data-id]");
      await withToastError(() => togglePaid(itemElement.dataset.type, itemElement.dataset.id, input.checked));
    });
  });
  screen.querySelectorAll('[data-action="edit-item"]').forEach((button) => {
    button.addEventListener("click", () => {
      const itemElement = button.closest("[data-id]");
      openEditDialog(itemElement.dataset.type, itemElement.dataset.id);
    });
  });
  screen.querySelectorAll('[data-action="delete-item"]').forEach((button) => {
    button.addEventListener("click", async () => {
      const itemElement = button.closest("[data-id]");
      await withToastError(() => deleteItem(itemElement.dataset.type, itemElement.dataset.id));
    });
  });
  screen.querySelectorAll('[data-action="adjust-saving"]').forEach((button) => {
    button.addEventListener("click", () => {
      const itemElement = button.closest("[data-id]");
      const saving = state.month.savings.find((item) => item.id === itemElement.dataset.id);
      openSavingDialog(saving);
    });
  });
}

function openEditDialog(type, id) {
  if (type === "donations") {
    openDonationDialog(state.month.donations.find((item) => item.id === id));
    return;
  }
  if (type === "variableIncomes") {
    openVariableIncomeDialog(state.month.variableIncomes.find((item) => item.id === id));
    return;
  }
  openExpenseDialog(type, state.month[type].find((item) => item.id === id));
}

function openBalanceDialog() {
  const month = state.month;
  const automatic = calculatedAvailableBalanceInUYU(month, state.config.incomeCurrency);
  openModal({
    title: "Disponible actual",
    body: `
      <form id="balance-form" class="form-grid">
        <div class="field">
          <label for="available-balance">Saldo actual en cuenta</label>
          <input id="available-balance" class="input" inputmode="decimal" value="${escapeAttr(toInput(availableBalanceInUYU(month, state.config.incomeCurrency)))}">
        </div>
        <p class="muted">Calculo automatico: ${formatUYU(automatic)}</p>
      </form>
    `,
    footer: `
      ${month.availableBalanceOverrideUYU != null ? `<button class="text-button" data-action="restore-auto" type="button">Restaurar automatico</button>` : ""}
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="balance-form" type="submit">Guardar</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#balance-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        await withToastError(async () => {
          await saveMonth({ ...state.month, availableBalanceOverrideUYU: parseAmount(dialog.querySelector("#available-balance").value) });
          closeModal();
          toastMessage("Disponible actualizado");
        });
      });
      dialog.querySelector('[data-action="restore-auto"]')?.addEventListener("click", async () => {
        await withToastError(async () => {
          await saveMonth({ ...state.month, availableBalanceOverrideUYU: null });
          closeModal();
          toastMessage("Calculo automatico restaurado");
        });
      });
    }
  });
}

function openIncomeDialog() {
  const currency = state.config.incomeCurrency;
  const currentValue = primaryIncomeAmountFor(state.month, currency);
  openModal({
    title: "Ingreso principal",
    body: `
      <form id="income-form" class="form-grid">
        <div class="field">
          <label for="primary-income">Monto ${currency}</label>
          <input id="primary-income" class="input" inputmode="decimal" value="${escapeAttr(toInput(currentValue))}">
        </div>
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="income-form" type="submit">Guardar</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#income-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const value = parseAmount(dialog.querySelector("#primary-income").value);
        const updated = currency === INCOME_CURRENCY.UYU
          ? { ...state.month, primaryIncomeUYUValue: value }
          : { ...state.month, primaryIncomeUSD: value };
        await withToastError(async () => {
          await saveMonth(updated);
          closeModal();
          toastMessage("Ingreso actualizado");
        });
      });
    }
  });
}

function openRatesDialog() {
  openModal({
    title: "Tipo de cambio",
    body: `
      <form id="rates-form" class="form-grid">
        <div class="inline-fields">
          <div class="field">
            <label for="purchase-rate">Compra</label>
            <input id="purchase-rate" class="input" inputmode="decimal" value="${escapeAttr(toInput(state.month.exchangeRate))}">
          </div>
          <div class="field">
            <label for="card-rate">Tarjeta</label>
            <input id="card-rate" class="input" inputmode="decimal" value="${escapeAttr(toInput(cardExchangeRate(state.month)))}">
          </div>
        </div>
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="rates-form" type="submit">Guardar</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#rates-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const purchase = parseAmount(dialog.querySelector("#purchase-rate").value);
        const card = parseAmount(dialog.querySelector("#card-rate").value);
        await withToastError(async () => {
          await saveMonth({
            ...state.month,
            exchangeRate: purchase,
            cardExchangeOffset: Math.max(0, card - purchase)
          });
          closeModal();
          toastMessage("Tipo de cambio actualizado");
        });
      });
    }
  });
}

function openVariableIncomeDialog(item = null) {
  openModal({
    title: item ? "Editar ingreso variable" : "Nuevo ingreso variable",
    body: moneyEntryForm("variable-income-form", item, "Ingreso"),
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="variable-income-form" type="submit">Guardar</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#variable-income-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const entry = readMoneyEntry(dialog, item);
        await withToastError(async () => {
          await upsertArrayItem("variableIncomes", entry);
          closeModal();
          toastMessage("Ingreso guardado");
        });
      });
    }
  });
}

function openExpenseDialog(field, item = null) {
  const titleByField = {
    fixedExpenses: item ? "Editar gasto fijo" : "Nuevo gasto fijo",
    variableExpenses: item ? "Editar gasto variable" : "Nuevo gasto variable",
    cardExpenses: item ? "Editar gasto de tarjeta" : "Nuevo gasto de tarjeta",
    debts: item ? "Editar deuda" : "Nueva deuda"
  };
  openModal({
    title: titleByField[field],
    body: expenseForm(field, item),
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="expense-form" type="submit">Guardar</button>
    `,
    bind: (dialog) => {
      const kindField = dialog.querySelector("#expense-kind");
      const installmentFields = dialog.querySelector("#installment-fields");
      kindField?.addEventListener("change", () => {
        installmentFields?.classList.toggle("hidden", kindField.value !== CARD_KIND.INSTALLMENT);
      });
      dialog.querySelector("#expense-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const entry = readExpenseEntry(dialog, field, item);
        const applyToFuture = Boolean(dialog.querySelector("#apply-to-future")?.checked);
        await withToastError(async () => {
          await upsertArrayItem(field, entry, { applyToFuture });
          closeModal();
          toastMessage(applyToFuture ? "Movimiento guardado en meses futuros" : "Movimiento guardado");
        });
      });
    }
  });
}

function openDonationDialog(item = null) {
  const isPercent = item?.percentOfPrimaryIncome != null;
  openModal({
    title: item ? "Editar donativo" : "Nuevo donativo",
    body: `
      <form id="donation-form" class="form-grid">
        <div class="field">
          <label for="money-name">Nombre</label>
          <input id="money-name" class="input" value="${escapeAttr(item?.name || "")}" required>
        </div>
        <div class="field">
          <label for="donation-mode">Modo</label>
          <select id="donation-mode" class="select">
            <option value="amount" ${!isPercent ? "selected" : ""}>Monto</option>
            <option value="percent" ${isPercent ? "selected" : ""}>Porcentaje del ingreso</option>
          </select>
        </div>
        <div id="donation-currency-row" class="field ${isPercent ? "hidden" : ""}">
          <label for="money-currency">Moneda</label>
          <select id="money-currency" class="select">
            <option value="UYU" ${!isInUSD(item || {}) ? "selected" : ""}>UYU</option>
            <option value="USD" ${isInUSD(item || {}) ? "selected" : ""}>USD</option>
          </select>
        </div>
        <div class="field">
          <label for="money-amount">Valor</label>
          <input id="money-amount" class="input" inputmode="decimal" value="${escapeAttr(toInput(isPercent ? item.percentOfPrimaryIncome : entryAmount(item || {})))}">
        </div>
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="donation-form" type="submit">Guardar</button>
    `,
    bind: (dialog) => {
      const mode = dialog.querySelector("#donation-mode");
      mode.addEventListener("change", () => {
        dialog.querySelector("#donation-currency-row").classList.toggle("hidden", mode.value === "percent");
      });
      dialog.querySelector("#donation-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const percentMode = mode.value === "percent";
        const currency = dialog.querySelector("#money-currency").value;
        const amount = parseAmount(dialog.querySelector("#money-amount").value);
        const donation = {
          ...(item || {}),
          id: item?.id || makeId(),
          name: dialog.querySelector("#money-name").value.trim(),
          amountUSD: !percentMode && currency === "USD" ? amount : 0,
          amountUYU: !percentMode && currency === "UYU" ? amount : 0,
          usd: !percentMode && currency === "USD",
          currency: percentMode ? "" : currency,
          percentOfPrimaryIncome: percentMode ? amount : null,
          paid: isPaid(item || {})
        };
        await withToastError(async () => {
          await upsertArrayItem("donations", donation);
          closeModal();
          toastMessage("Donativo guardado");
        });
      });
    }
  });
}

function openSavingDialog(saving) {
  openModal({
    title: "Ajustar ahorro",
    body: `
      <form id="saving-form" class="form-grid">
        <div class="field">
          <label for="saving-name">Nombre</label>
          <input id="saving-name" class="input" value="${escapeAttr(saving?.name || "Ahorro")}" required>
        </div>
        <div class="inline-fields">
          <div class="field">
            <label for="saving-direction">Movimiento</label>
            <select id="saving-direction" class="select">
              <option value="add">Sumar</option>
              <option value="subtract">Restar</option>
            </select>
          </div>
          <div class="field">
            <label for="saving-currency">Moneda</label>
            <select id="saving-currency" class="select">
              <option value="UYU" ${saving?.currency !== "USD" ? "selected" : ""}>UYU</option>
              <option value="USD" ${saving?.currency === "USD" ? "selected" : ""}>USD</option>
            </select>
          </div>
          <div class="field">
            <label for="saving-amount">Monto</label>
            <input id="saving-amount" class="input" inputmode="decimal">
          </div>
        </div>
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="saving-form" type="submit">Guardar</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#saving-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const direction = dialog.querySelector("#saving-direction").value;
        const currency = dialog.querySelector("#saving-currency").value;
        const amount = parseAmount(dialog.querySelector("#saving-amount").value);
        const delta = direction === "subtract" ? -amount : amount;
        await withToastError(async () => {
          await adjustSaving(saving, dialog.querySelector("#saving-name").value.trim(), delta, currency);
          closeModal();
          toastMessage("Ahorro actualizado");
        });
      });
    }
  });
}

function moneyEntryForm(formId, item = null, nameLabel = "Nombre") {
  return `
    <form id="${formId}" class="form-grid">
      <div class="field">
        <label for="money-name">${nameLabel}</label>
        <input id="money-name" class="input" value="${escapeAttr(item?.name || "")}" required>
      </div>
      <div class="field">
        <label for="money-category">Categoria</label>
        <input id="money-category" class="input" list="category-options" value="${escapeAttr(item?.category || "")}">
        ${categoryDatalist()}
      </div>
      <div class="inline-fields">
        <div class="field">
          <label for="money-currency">Moneda</label>
          <select id="money-currency" class="select">
            <option value="UYU" ${!isInUSD(item || {}) ? "selected" : ""}>UYU</option>
            <option value="USD" ${isInUSD(item || {}) ? "selected" : ""}>USD</option>
          </select>
        </div>
        <div class="field">
          <label for="money-amount">Monto</label>
          <input id="money-amount" class="input" inputmode="decimal" value="${escapeAttr(toInput(entryAmount(item || {})))}">
        </div>
      </div>
    </form>
  `;
}

function expenseForm(field, item = null) {
  const base = moneyEntryForm("expense-form", item, "Nombre");
  const showApplyFuture = field === "fixedExpenses" || field === "cardExpenses";
  const defaultApplyFuture = field === "fixedExpenses" && item == null;
  const cardExtras = field === "cardExpenses" ? `
    <div class="field">
      <label for="expense-kind">Tipo</label>
      <select id="expense-kind" class="select">
        <option value="PUNCTUAL" ${item?.kind !== "RECURRING" && item?.kind !== "INSTALLMENT" ? "selected" : ""}>Puntual</option>
        <option value="RECURRING" ${item?.kind === "RECURRING" ? "selected" : ""}>Recurrente</option>
        <option value="INSTALLMENT" ${item?.kind === "INSTALLMENT" ? "selected" : ""}>Cuotas</option>
      </select>
    </div>
  ` : "";
  const futureExtras = showApplyFuture ? `
    <label class="checkbox-line">
      <input id="apply-to-future" type="checkbox" ${defaultApplyFuture ? "checked" : ""}>
      <span>Aplicar a los meses siguientes</span>
    </label>
  ` : "";
  const installmentExtras = field === "cardExpenses" || field === "debts" ? `
    <div id="installment-fields" class="inline-fields ${field === "cardExpenses" && item?.kind !== "INSTALLMENT" ? "hidden" : ""}">
      <div class="field">
        <label for="current-installment">Cuota actual</label>
        <input id="current-installment" class="input" inputmode="numeric" value="${escapeAttr(String(item?.currentInstallment || 1))}">
      </div>
      <div class="field">
        <label for="total-installments">Total cuotas</label>
        <input id="total-installments" class="input" inputmode="numeric" value="${escapeAttr(String(item?.totalInstallments || 1))}">
      </div>
    </div>
  ` : "";
  return base.replace("</form>", `${cardExtras}${installmentExtras}${futureExtras}</form>`);
}

function readMoneyEntry(root, item = null) {
  const currency = root.querySelector("#money-currency").value;
  const amount = parseAmount(root.querySelector("#money-amount").value);
  return {
    ...(item || {}),
    id: item?.id || makeId(),
    name: root.querySelector("#money-name").value.trim(),
    category: root.querySelector("#money-category")?.value.trim() || "",
    amountUSD: currency === "USD" ? amount : 0,
    amountUYU: currency === "UYU" ? amount : 0,
    usd: currency === "USD",
    currency,
    paid: isPaid(item || {})
  };
}

function readExpenseEntry(root, field, item = null) {
  const entry = readMoneyEntry(root, item);
  if (field === "fixedExpenses") {
    entry.pinned = true;
  }
  if (field === "cardExpenses") {
    entry.kind = root.querySelector("#expense-kind").value;
    entry.totalInstallments = entry.kind === CARD_KIND.INSTALLMENT
      ? parseInt(root.querySelector("#total-installments").value, 10) || 1
      : 1;
    entry.currentInstallment = entry.kind === CARD_KIND.INSTALLMENT
      ? parseInt(root.querySelector("#current-installment").value, 10) || 1
      : 1;
  }
  if (field === "debts") {
    entry.totalInstallments = parseInt(root.querySelector("#total-installments").value, 10) || 1;
    entry.currentInstallment = parseInt(root.querySelector("#current-installment").value, 10) || 1;
  }
  return entry;
}

async function createFamily(config) {
  const uid = state.user.uid;
  const joinCode = await generateJoinCode();
  const family = { id: uid, joinCode, memberIds: [uid] };
  await setDoc(familyRef(uid), family);
  await setDoc(doc(db, JOIN_CODES_COLLECTION, joinCode), { familyId: uid });
  await setDoc(doc(db, "families", uid, "config", "main"), config);
  activateFamily(family);
  await ensureMonthDocument(state.yearMonth);
  render();
}

async function joinFamily(code) {
  if (!/^\d{6}$/.test(code)) throw new Error("El codigo debe tener 6 digitos.");
  const joinSnap = await getDoc(doc(db, JOIN_CODES_COLLECTION, code));
  if (!joinSnap.exists()) throw new Error("Codigo de familia invalido.");
  const familyId = joinSnap.data().familyId;
  await updateDoc(familyRef(familyId), { memberIds: arrayUnion(state.user.uid) });
  const familySnap = await getDoc(familyRef(familyId));
  if (!familySnap.exists()) throw new Error("No se pudo abrir la familia.");
  activateFamily(familySnap.data());
  await ensureMonthDocument(state.yearMonth);
  render();
}

async function generateJoinCode() {
  for (let index = 0; index < 20; index += 1) {
    const code = String(Math.floor(100000 + Math.random() * 900000));
    const exists = (await getDoc(doc(db, JOIN_CODES_COLLECTION, code))).exists();
    if (!exists) return code;
  }
  return makeId().replace(/\D/g, "").slice(0, 6).padEnd(6, "0");
}

async function ensureMonthDocument(yearMonth) {
  const existing = await getDoc(monthRef(yearMonth));
  if (existing.exists()) return normalizeMonth(existing.data(), yearMonth);

  const previousKey = addMonths(yearMonth, -1);
  const previousSnap = await getDoc(monthRef(previousKey)).catch(() => null);
  const previous = previousSnap?.exists() ? normalizeMonth(previousSnap.data(), previousKey) : null;
  const config = state.config || defaultConfig;
  const month = previous
    ? rolloverMonth(previous, yearMonth)
    : {
        yearMonth,
        exchangeRate: Number(config.defaultExchangeRate || 0),
        cardExchangeOffset: Number(config.defaultCardExchangeOffset || 0),
        primaryIncomeUSD: 0,
        primaryIncomeUYUValue: 0,
        variableIncomes: [],
        donations: [],
        savings: defaultSavings(config),
        fixedExpenses: [],
        variableExpenses: [],
        cardExpenses: [],
        debts: [],
        availableBalanceOverrideUYU: null
      };
  await setDoc(monthRef(yearMonth), month);
  return month;
}

function rolloverMonth(previous, yearMonth) {
  return {
    yearMonth,
    exchangeRate: previous.exchangeRate,
    cardExchangeOffset: previous.cardExchangeOffset,
    primaryIncomeUSD: previous.primaryIncomeUSD,
    primaryIncomeUYUValue: previous.primaryIncomeUYUValue,
    variableIncomes: [],
    donations: previous.donations.filter((item) => item.percentOfPrimaryIncome != null).map(asPending),
    savings: previous.savings,
    fixedExpenses: previous.fixedExpenses.map(asPending),
    variableExpenses: [],
    cardExpenses: previous.cardExpenses.map(advanceCardExpense).filter(Boolean),
    debts: previous.debts.map(advanceDebt).filter(Boolean),
    availableBalanceOverrideUYU: null
  };
}

function advanceCardExpense(item) {
  if (item.kind === CARD_KIND.RECURRING) return asPending(item);
  if (item.kind !== CARD_KIND.INSTALLMENT) return null;
  const next = Number(item.currentInstallment || 1) + 1;
  return next > Number(item.totalInstallments || 1)
    ? null
    : { ...asPending(item), currentInstallment: next };
}

function advanceDebt(item) {
  const next = Number(item.currentInstallment || 1) + 1;
  return next > Number(item.totalInstallments || 1)
    ? null
    : { ...asPending(item), currentInstallment: next };
}

function asPending(item) {
  return { ...item, paid: false };
}

function openGenerateMonthsDialog() {
  openModal({
    title: "Agregar meses",
    body: `
      <form id="generate-months-form" class="form-grid">
        <div class="field">
          <label for="months-count">Cantidad de meses</label>
          <input id="months-count" class="input" inputmode="numeric" min="1" max="36" value="1" required>
        </div>
        <p class="muted">Se crean meses consecutivos desde ${escapeHtml(formatMonthLabel(state.yearMonth))}, heredando gastos fijos, tarjeta, deudas, donativos recurrentes y ahorros.</p>
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="generate-months-form" type="submit">Agregar</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#generate-months-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const count = Math.max(1, Math.min(36, parseInt(dialog.querySelector("#months-count").value, 10) || 1));
        await withToastError(async () => {
          const lastKey = await generateFutureMonths(count);
          closeModal();
          setMonth(lastKey);
          toastMessage(count === 1 ? "Mes agregado" : `${count} meses agregados`);
        });
      });
    }
  });
}

function openDeleteMonthsDialog() {
  const affectedMonths = monthKeysFrom(state.yearMonth);
  const affectedCount = affectedMonths.length;
  openModal({
    title: "Eliminar meses",
    body: `
      <form id="delete-months-form" class="form-grid">
        <p class="muted">Se eliminara ${escapeHtml(formatMonthLabel(state.yearMonth))} y todos los meses posteriores. Los meses anteriores quedan intactos.</p>
        <div class="metric-grid">
          ${metricPill("Mes inicial", formatMonthLabel(state.yearMonth))}
          ${metricPill("Meses afectados", String(affectedCount))}
        </div>
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="danger-button" form="delete-months-form" type="submit">Eliminar</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#delete-months-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        await withToastError(async () => {
          const nextSelectedMonth = await deleteMonthsFrom(state.yearMonth);
          closeModal();
          setMonth(nextSelectedMonth);
          toastMessage(affectedCount === 1 ? "Mes eliminado" : `${affectedCount} meses eliminados`);
        });
      });
    }
  });
}

async function generateFutureMonths(monthsAhead) {
  if (monthsAhead <= 0) return state.yearMonth;
  const lastAvailableKey = state.availableMonths.slice().sort().at(-1);
  const startKey = !lastAvailableKey || lastAvailableKey < state.yearMonth
    ? state.yearMonth
    : lastAvailableKey;
  let current = await ensureMonthDocument(startKey);
  let lastCreatedKey = current.yearMonth;
  for (let index = 0; index < monthsAhead; index += 1) {
    const nextKey = addMonths(current.yearMonth, 1);
    const existingSnap = await getDoc(monthRef(nextKey));
    if (existingSnap.exists()) {
      current = normalizeMonth(existingSnap.data(), nextKey);
      lastCreatedKey = current.yearMonth;
    } else {
      const nextMonth = rolloverMonth(current, nextKey);
      await setDoc(monthRef(nextKey), nextMonth);
      current = nextMonth;
      lastCreatedKey = nextKey;
    }
  }
  if ((state.config.planningThroughYearMonth || "") < lastCreatedKey) {
    await setDoc(configRef(), {
      ...state.config,
      planningThroughYearMonth: lastCreatedKey
    });
  }
  return lastCreatedKey;
}

async function deleteMonthsFrom(yearMonth) {
  const currentKey = currentYearMonth();
  const monthKeys = monthKeysFrom(yearMonth);
  const remainingMonths = [...new Set([...state.availableMonths, yearMonth])]
    .filter((key) => key < yearMonth)
    .sort();
  const nextSelectedMonth = remainingMonths.at(-1) || currentKey;
  const newPlanningLimit = remainingMonths.at(-1) || "";

  state.isDeletingMonths = true;
  renderChrome();
  state.monthUnsubscribe?.();
  state.monthUnsubscribe = null;

  try {
    for (const key of monthKeys) {
      await deleteDoc(monthRef(key));
    }
    if ((state.config.planningThroughYearMonth || "") !== newPlanningLimit) {
      await setDoc(configRef(), {
        ...state.config,
        planningThroughYearMonth: newPlanningLimit
      });
    }
    if (nextSelectedMonth === currentKey && !remainingMonths.includes(currentKey)) {
      await ensureMonthDocument(currentKey);
    }
    return nextSelectedMonth;
  } finally {
    state.isDeletingMonths = false;
    renderChrome();
  }
}

function monthKeysFrom(yearMonth) {
  return [...new Set([...state.availableMonths, yearMonth])]
    .filter((key) => key >= yearMonth)
    .sort();
}

function goToNearestMonth(direction) {
  const months = [...new Set([...state.availableMonths, state.yearMonth])].sort();
  const currentIndex = months.indexOf(state.yearMonth);
  const target = months[currentIndex + direction] || addMonths(state.yearMonth, direction);
  setMonth(target);
}

function setMonth(yearMonth) {
  state.yearMonth = yearMonth;
  if (state.family) subscribeMonth(yearMonth);
  render();
}

function openMonthPicker() {
  const months = [...new Set([...state.availableMonths, state.yearMonth, currentYearMonth()])].sort();
  openModal({
    title: "Elegir mes",
    body: `
      <form id="month-form" class="form-grid">
        <div class="field">
          <label for="month-select">Mes</label>
          <select id="month-select" class="select">
            ${months.map((item) => `<option value="${item}" ${item === state.yearMonth ? "selected" : ""}>${escapeHtml(formatMonthLabel(item))}</option>`).join("")}
          </select>
        </div>
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="month-form" type="submit">Ir</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#month-form").addEventListener("submit", (event) => {
        event.preventDefault();
        setMonth(dialog.querySelector("#month-select").value);
        closeModal();
      });
    }
  });
}

async function saveMonth(month) {
  const clean = normalizeMonth(month, month.yearMonth || state.yearMonth);
  await setDoc(monthRef(clean.yearMonth), clean);
}

async function upsertArrayItem(field, item, options = {}) {
  const list = [...(state.month[field] || [])];
  const index = list.findIndex((entry) => entry.id === item.id);
  if (index >= 0) list[index] = item;
  else list.push(item);
  await saveMonth({ ...state.month, [field]: list });
  if (options.applyToFuture) {
    await applyItemToFutureMonths(field, item);
  }
}

async function applyItemToFutureMonths(field, item) {
  const futureKeys = state.availableMonths.filter((key) => key > state.yearMonth).sort();
  if (field === "fixedExpenses") {
    for (const key of futureKeys) {
      const snap = await getDoc(monthRef(key));
      if (!snap.exists()) continue;
      const future = normalizeMonth(snap.data(), key);
      const items = [...future.fixedExpenses];
      const index = items.findIndex((entry) => entry.id === item.id);
      if (index >= 0) {
        items[index] = { ...item, pinned: true, paid: isPaid(items[index]) };
      } else {
        items.push({ ...item, pinned: true, paid: false });
      }
      await setDoc(monthRef(key), { ...future, fixedExpenses: items });
    }
  }
  if (field === "cardExpenses") {
    let propagated = advanceCardExpense(item);
    for (const key of futureKeys) {
      const snap = await getDoc(monthRef(key));
      if (!snap.exists()) continue;
      const future = normalizeMonth(snap.data(), key);
      const items = [...future.cardExpenses];
      const index = items.findIndex((entry) => entry.id === item.id);
      let changed = false;
      if (propagated) {
        const nextItem = index >= 0
          ? { ...propagated, paid: isPaid(items[index]) }
          : { ...propagated, paid: false };
        if (index >= 0) items[index] = nextItem;
        else items.push(nextItem);
        changed = true;
      } else if (index >= 0) {
        items.splice(index, 1);
        changed = true;
      }
      if (changed) {
        await setDoc(monthRef(key), { ...future, cardExpenses: items });
      }
      propagated = propagated ? advanceCardExpense(propagated) : null;
    }
  }
}

async function deleteItem(field, id) {
  if (!confirm("Eliminar este movimiento?")) return;
  await saveMonth({ ...state.month, [field]: state.month[field].filter((item) => item.id !== id) });
  toastMessage("Eliminado");
}

async function togglePaid(field, id, paid) {
  await saveMonth({
    ...state.month,
    [field]: state.month[field].map((item) => item.id === id ? { ...item, paid } : item)
  });
}

async function adjustSaving(saving, newName, deltaAmount, currency) {
  const id = saving?.id || makeId();
  const updateSaving = (current) => {
    const base = current || { id, name: newName, amountUYU: 0, amountUSD: 0, currency };
    const isUsd = (base.currency || currency) === "USD";
    return {
      ...base,
      id,
      name: newName,
      currency: base.currency || currency,
      amountUSD: isUsd ? Math.max(0, Number(base.amountUSD || 0) + deltaAmount) : Number(base.amountUSD || 0),
      amountUYU: !isUsd ? Math.max(0, Number(base.amountUYU || 0) + deltaAmount) : Number(base.amountUYU || 0),
      lastUpdated: new Date()
    };
  };

  const currentSavings = [...state.month.savings];
  const index = currentSavings.findIndex((item) => item.id === id);
  if (index >= 0) currentSavings[index] = updateSaving(currentSavings[index]);
  else if (deltaAmount > 0) currentSavings.push(updateSaving(null));
  await saveMonth({ ...state.month, savings: currentSavings });

  const futureKeys = state.availableMonths.filter((key) => key > state.yearMonth);
  for (const key of futureKeys) {
    const snap = await getDoc(monthRef(key));
    if (!snap.exists()) continue;
    const future = normalizeMonth(snap.data(), key);
    const futureSavings = [...future.savings];
    const futureIndex = futureSavings.findIndex((item) => item.id === id);
    if (futureIndex >= 0) futureSavings[futureIndex] = updateSaving(futureSavings[futureIndex]);
    else if (deltaAmount > 0) futureSavings.push(updateSaving(null));
    await setDoc(monthRef(key), { ...future, savings: futureSavings });
  }
}

function familyRef(familyId) {
  return doc(db, "families", familyId);
}

function configRef() {
  return doc(db, "families", state.family.id, "config", "main");
}

function monthsCollectionRef() {
  return collection(db, "families", state.family.id, "months");
}

function monthRef(yearMonth) {
  return doc(db, "families", state.family.id, "months", yearMonth);
}

function normalizeConfig(config) {
  return {
    ...defaultConfig,
    ...config,
    expenseCategories: mergeCategories(config.expenseCategories || defaultCategories)
  };
}

function normalizeMonth(month, yearMonth) {
  return {
    yearMonth,
    exchangeRate: Number(month.exchangeRate || 0),
    cardExchangeOffset: Number(month.cardExchangeOffset ?? 2),
    primaryIncomeUSD: Number(month.primaryIncomeUSD || 0),
    primaryIncomeUYUValue: Number(month.primaryIncomeUYUValue || 0),
    variableIncomes: month.variableIncomes || [],
    donations: month.donations || [],
    savings: month.savings?.length ? month.savings : defaultSavings(state.config),
    fixedExpenses: month.fixedExpenses || [],
    variableExpenses: month.variableExpenses || [],
    cardExpenses: month.cardExpenses || [],
    debts: month.debts || [],
    availableBalanceOverrideUYU: month.availableBalanceOverrideUYU ?? null
  };
}

function defaultSavings(config) {
  return [
    { id: "saving_1", name: config.saving1Name || "Ahorro 1", amountUYU: 0, amountUSD: 0, currency: "UYU", lastUpdated: null },
    { id: "saving_2", name: config.saving2Name || "Ahorro 2", amountUYU: 0, amountUSD: 0, currency: "UYU", lastUpdated: null },
    { id: "saving_3", name: config.saving3Name || "Ahorro 3", amountUYU: 0, amountUSD: 0, currency: "UYU", lastUpdated: null }
  ];
}

function readSetupConfig() {
  const purchase = parseAmount(document.querySelector("#setup-purchase").value);
  const sale = parseAmount(document.querySelector("#setup-sale").value);
  return {
    ...defaultConfig,
    incomeCurrency: document.querySelector("#setup-currency").value,
    defaultExchangeRate: purchase,
    defaultCardExchangeOffset: Math.max(0, sale - purchase),
    marginGreenThresholdPct: parseAmount(document.querySelector("#setup-green").value) || 20000,
    marginYellowThresholdPct: parseAmount(document.querySelector("#setup-yellow").value) || 5000
  };
}

function primaryIncomeAmountFor(month, currency) {
  return currency === INCOME_CURRENCY.UYU ? Number(month.primaryIncomeUYUValue || 0) : Number(month.primaryIncomeUSD || 0);
}

function primaryIncomeInUYU(month, currency) {
  return currency === INCOME_CURRENCY.UYU
    ? Number(month.primaryIncomeUYUValue || 0)
    : Number(month.primaryIncomeUSD || 0) * Number(month.exchangeRate || 0);
}

function variableIncomeUYU(month) {
  return sumEntries(month.variableIncomes, Number(month.exchangeRate || 0));
}

function totalIncomeInUYU(month, currency) {
  return primaryIncomeInUYU(month, currency) + variableIncomeUYU(month);
}

function donationsInUYU(month, currency) {
  const primaryIncome = primaryIncomeInUYU(month, currency);
  return month.donations.reduce((sum, item) => sum + donationTotalUYU(item, primaryIncome, cardExchangeRate(month)), 0);
}

function totalObligationsInUYU(month, currency) {
  const rate = cardExchangeRate(month);
  return donationsInUYU(month, currency)
    + sumEntries(month.fixedExpenses, rate)
    + sumEntries(month.variableExpenses, rate)
    + sumEntries(month.cardExpenses, rate)
    + sumEntries(month.debts, rate);
}

function paidObligationsInUYU(month, currency) {
  const primaryIncome = primaryIncomeInUYU(month, currency);
  const rate = cardExchangeRate(month);
  return month.donations.filter(isPaid).reduce((sum, item) => sum + donationTotalUYU(item, primaryIncome, rate), 0)
    + sumEntries(month.fixedExpenses.filter(isPaid), rate)
    + sumEntries(month.variableExpenses.filter(isPaid), rate)
    + sumEntries(month.cardExpenses.filter(isPaid), rate)
    + sumEntries(month.debts.filter(isPaid), rate);
}

function pendingObligationsInUYU(month, currency) {
  const primaryIncome = primaryIncomeInUYU(month, currency);
  const rate = cardExchangeRate(month);
  return month.donations.filter((item) => !isPaid(item)).reduce((sum, item) => sum + donationTotalUYU(item, primaryIncome, rate), 0)
    + sumEntries(month.fixedExpenses.filter((item) => !isPaid(item)), rate)
    + sumEntries(month.variableExpenses.filter((item) => !isPaid(item)), rate)
    + sumEntries(month.cardExpenses.filter((item) => !isPaid(item)), rate)
    + sumEntries(month.debts.filter((item) => !isPaid(item)), rate);
}

function calculatedAvailableBalanceInUYU(month, currency) {
  return totalIncomeInUYU(month, currency) - paidObligationsInUYU(month, currency);
}

function availableBalanceInUYU(month, currency) {
  return month.availableBalanceOverrideUYU ?? calculatedAvailableBalanceInUYU(month, currency);
}

function availableMarginInUYU(month, currency) {
  return availableBalanceInUYU(month, currency) - pendingObligationsInUYU(month, currency);
}

function cardExchangeRate(month) {
  return Number(month.exchangeRate || 0) + Number(month.cardExchangeOffset || 0);
}

function donationTotalUYU(item, primaryIncomeUYU, rate) {
  if (item.percentOfPrimaryIncome != null) {
    return primaryIncomeUYU * (Number(item.percentOfPrimaryIncome || 0) / 100);
  }
  return totalEntryUYU(item, rate);
}

function sumEntries(items, rate) {
  return items.reduce((sum, item) => sum + totalEntryUYU(item, rate), 0);
}

function totalEntryUYU(item, rate) {
  return isInUSD(item) ? Number(item.amountUSD || 0) * Number(rate || 0) : Number(item.amountUYU || 0);
}

function entryAmount(item) {
  return isInUSD(item) ? Number(item.amountUSD || 0) : Number(item.amountUYU || 0);
}

function isInUSD(item) {
  if (!item) return false;
  if (item.currency === INCOME_CURRENCY.USD) return true;
  if (item.currency === INCOME_CURRENCY.UYU) return false;
  return Boolean(item.usd ?? item.isUSD ?? (Number(item.amountUSD || 0) !== 0 && Number(item.amountUYU || 0) === 0));
}

function isPaid(item) {
  return Boolean(item?.paid ?? item?.isPaid);
}

function savingsTotals(month) {
  return month.savings.reduce((totals, saving) => {
    if (saving.currency === "USD") totals.usd += Number(saving.amountUSD || 0);
    else totals.uyu += Number(saving.amountUYU || 0);
    return totals;
  }, { uyu: 0, usd: 0 });
}

function categoryTotals(month) {
  const totals = new Map();
  const add = (item) => {
    const category = item.category || "Sin categoria";
    totals.set(category, (totals.get(category) || 0) + totalEntryUYU(item, cardExchangeRate(month)));
  };
  [...month.fixedExpenses, ...month.variableExpenses, ...month.cardExpenses, ...month.debts].forEach(add);
  const colors = ["#00897b", "#3949ab", "#d81b60", "#f4511e", "#7cb342", "#5e35b1", "#039be5", "#8e24aa"];
  return [...totals.entries()]
    .filter(([, value]) => value > 0)
    .sort((a, b) => b[1] - a[1])
    .map(([label, value], index) => [label, value, colors[index % colors.length]]);
}

function marginColorClass(amount, config) {
  if (amount >= Number(config.marginGreenThresholdPct || 20000)) return "green-zone";
  if (amount >= Number(config.marginYellowThresholdPct || 5000)) return "yellow-zone";
  return "red-zone";
}

function labelCardKind(kind) {
  if (kind === CARD_KIND.RECURRING) return "Recurrente";
  if (kind === CARD_KIND.INSTALLMENT) return "Cuotas";
  return "Puntual";
}

function metricPill(label, value) {
  return `
    <div class="metric-pill">
      <span class="metric-label">${escapeHtml(label)}</span>
      <strong class="metric-value">${escapeHtml(value)}</strong>
    </div>
  `;
}

function sectionTitle(title, icon, trailing) {
  return `
    <div class="section-title">
      <div class="section-title-left">
        <span class="soft-badge">${escapeHtml(icon)}</span>
        <h3>${escapeHtml(title)}</h3>
      </div>
      <div>${trailing || ""}</div>
    </div>
  `;
}

function themeChip(value, label) {
  const active = state.appearance.themeMode === value ? "active" : "";
  return `<button class="filter-chip ${active}" data-theme-mode="${escapeAttr(value)}" type="button">${escapeHtml(label)}</button>`;
}

function accentChip(value, label) {
  const active = state.appearance.accentColor === value ? "active" : "";
  return `
    <button class="filter-chip accent-choice ${active}" data-accent-color="${escapeAttr(value)}" type="button">
      <span class="accent-swatch accent-${escapeAttr(value)}"></span>
      ${escapeHtml(label)}
    </button>
  `;
}

function emptyState(text) {
  return `<p class="muted">${escapeHtml(text)}</p>`;
}

function loadingPanel(text) {
  return `<div class="loading-panel"><div class="spinner"></div><span>${escapeHtml(text)}</span></div>`;
}

function barList(items) {
  const max = Math.max(1, ...items.map((item) => Math.abs(item[1])));
  return `
    <div class="bar-list">
      ${items.map(([label, value, color]) => `
        <div class="bar-row">
          <div class="bar-heading">
            <strong>${escapeHtml(label)}</strong>
            <span>${formatUYU(value)}</span>
          </div>
          <div class="bar-track"><div class="bar-fill" style="width: ${Math.min(100, Math.abs(value) / max * 100)}%; background: ${color};"></div></div>
        </div>
      `).join("")}
    </div>
  `;
}

function comparisonList(items) {
  const max = Math.max(1, ...items.flatMap((item) => [Math.abs(item[1]), Math.abs(item[2])]));
  return `
    <div class="bar-list">
      ${items.map(([label, current, previous]) => `
        <div class="bar-row">
          <div class="bar-heading">
            <strong>${escapeHtml(label)}</strong>
            <span>${formatUYU(current - previous)}</span>
          </div>
          <div class="bar-track"><div class="bar-fill" style="width: ${Math.min(100, Math.abs(current) / max * 100)}%; background: var(--primary);"></div></div>
          <div class="bar-track"><div class="bar-fill" style="width: ${Math.min(100, Math.abs(previous) / max * 100)}%; background: var(--blue);"></div></div>
        </div>
      `).join("")}
    </div>
  `;
}

function categoryDatalist() {
  const categories = mergeCategories(state.config.expenseCategories || defaultCategories);
  return `<datalist id="category-options">${categories.map((category) => `<option value="${escapeAttr(category)}"></option>`).join("")}</datalist>`;
}

function mergeCategories(categories) {
  const normalized = new Map();
  [...defaultCategories, ...(categories || [])].forEach((category) => {
    const clean = String(category || "").trim();
    if (clean) normalized.set(clean.toLowerCase(), clean);
  });
  return [...normalized.values()];
}

function openModal({ title, body, footer, bind }) {
  modal.innerHTML = `
    <div class="modal-content">
      <div class="modal-header">
        <div>
          <p class="eyebrow">Finanzas Familiares</p>
          <h2>${escapeHtml(title)}</h2>
        </div>
        <button class="icon-button" data-action="cancel" type="button" title="Cerrar">×</button>
      </div>
      ${body}
      <div class="button-row">${footer}</div>
    </div>
  `;
  modal.querySelectorAll('[data-action="cancel"]').forEach((button) => button.addEventListener("click", closeModal));
  bind?.(modal);
  modal.showModal();
}

function closeModal() {
  modal.close();
  modal.innerHTML = "";
}

async function withToastError(task) {
  try {
    await task();
  } catch (error) {
    toastMessage(error?.message || "No se pudo completar la accion.");
    console.error(error);
  }
}

function toastMessage(message) {
  toast.textContent = message;
  toast.classList.add("visible");
  clearTimeout(toastMessage.timeoutId);
  toastMessage.timeoutId = setTimeout(() => toast.classList.remove("visible"), 2800);
}

function renderFatalError(error) {
  screen.innerHTML = `
    <div class="empty-panel">
      <strong>No se pudo conectar Firebase.</strong>
      <span>${escapeHtml(error?.message || "Revisa la configuracion del proyecto.")}</span>
    </div>
  `;
}

function formatUYU(value) {
  return `$ ${numberFormatter().format(Number(value || 0))}`;
}

function formatUSD(value) {
  return `U$S ${numberFormatter().format(Number(value || 0))}`;
}

function numberFormatter() {
  return new Intl.NumberFormat("es-UY", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function toInput(value) {
  return Number(value || 0).toFixed(2);
}

function parseAmount(value) {
  const clean = String(value || "")
    .replace("U$S", "")
    .replace("$", "")
    .replace(/\s/g, "")
    .replace(",", ".");
  return Number.parseFloat(clean) || 0;
}

function currentYearMonth() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

function addMonths(yearMonth, amount) {
  const [year, month] = yearMonth.split("-").map(Number);
  const date = new Date(year, month - 1 + amount, 1);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

function formatMonthLabel(yearMonth) {
  const [year, month] = yearMonth.split("-").map(Number);
  const date = new Date(year, month - 1, 1);
  const label = new Intl.DateTimeFormat("es-UY", { month: "long", year: "numeric" }).format(date);
  return label.charAt(0).toUpperCase() + label.slice(1);
}

function makeId() {
  return crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function escapeAttr(value) {
  return escapeHtml(value);
}
