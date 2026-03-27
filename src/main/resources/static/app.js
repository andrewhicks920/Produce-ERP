/**
 * ProduceERP — Frontend Application
 * Pure vanilla JS, no external dependencies.
 * Architecture: Config → API → Utils → Render → Modal → Notify → Views → Navigation
 */


const Config = {
  BASE_URL: '',          // Served by Spring Boot on same origin — no prefix needed
  TOAST_DURATION: 3000, // ms before auto-dismiss
  PO_STATUSES: ['DRAFT', 'SENT', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED'],
  TX_TYPES: ['PURCHASE', 'SALE', 'ADJUSTMENT'],
};


const API = {
  /**
   * Generic fetch wrapper with JSON parsing and error handling.
   * @param {string} path - Relative URL path (e.g. "/products")
   * @param {RequestInit} [options] - Fetch options
   * @returns {Promise<any>} Parsed JSON response, or null for 204 No Content
   */
  async apiFetch(path, options = {}) {
    const url = `${Config.BASE_URL}${path}`;
    const defaultHeaders = { 'Content-Type': 'application/json' };

    const config = {
      ...options,
      headers: { ...defaultHeaders, ...(options.headers || {}) },
    };

    // Only attach body for methods that allow it
    if (config.body && typeof config.body === 'object') {
      config.body = JSON.stringify(config.body);
    }

    const response = await fetch(url, config);

    if (!response.ok) {
      let message = `HTTP ${response.status} — ${response.statusText}`;
      try {
        const err = await response.json();
        message = err.message || err.error || message;
      } catch (_) { /* body may not be JSON */ }
      throw new Error(message);
    }

    if (response.status === 204) return null;
    return response.json();
  },

  getProducts:         ()       => API.apiFetch('/products'),
  getProduct:          (id)     => API.apiFetch(`/products/${id}`),
  createProduct:       (data)   => API.apiFetch('/products', { method: 'POST', body: data }),
  updateProduct:       (id, d)  => API.apiFetch(`/products/${id}`, { method: 'PUT', body: d }),
  deleteProduct:       (id)     => API.apiFetch(`/products/${id}`, { method: 'DELETE' }),

  getSuppliers:        ()       => API.apiFetch('/suppliers'),
  getSupplier:         (id)     => API.apiFetch(`/suppliers/${id}`),
  createSupplier:      (data)   => API.apiFetch('/suppliers', { method: 'POST', body: data }),
  updateSupplier:      (id, d)  => API.apiFetch(`/suppliers/${id}`, { method: 'PUT', body: d }),
  deleteSupplier:      (id)     => API.apiFetch(`/suppliers/${id}`, { method: 'DELETE' }),


  getPurchaseOrders:   (params) => API.apiFetch(`/purchase-orders${Utils.buildQuery(params)}`),
  getPurchaseOrder:    (id)     => API.apiFetch(`/purchase-orders/${id}`),
  createPurchaseOrder: (data)   => API.apiFetch('/purchase-orders', { method: 'POST', body: data }),
  updatePurchaseOrder: (id, d)  => API.apiFetch(`/purchase-orders/${id}`, { method: 'PUT', body: d }),
  patchPOStatus:       (id, s)  => API.apiFetch(`/purchase-orders/${id}/status`, { method: 'PATCH', body: { status: s } }),
  deletePurchaseOrder: (id)     => API.apiFetch(`/purchase-orders/${id}`, { method: 'DELETE' }),


  getTransactions:     ()       => API.apiFetch('/transactions'),
  createTransaction:   (data)   => API.apiFetch('/transactions', { method: 'POST', body: data }),
};


const Utils = {
  /**
   * Build a URL query string from a params object, omitting falsy values.
   * @param {Object} [params]
   * @returns {string}
   */
  buildQuery(params) {
    if (!params) return '';
    const q = new URLSearchParams();
    for (const [k, v] of Object.entries(params)) {
      if (v !== '' && v !== null && v !== undefined) q.set(k, v);
    }
    const s = q.toString();
    return s ? `?${s}` : '';
  },

  /**
   * Format an ISO date string to a human-readable local date.
   * @param {string|null} dateStr
   * @returns {string}
   */
  formatDate(dateStr) {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    if (isNaN(d)) return dateStr;
    return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  },

  /**
   * Format a number as USD currency.
   * @param {number|string|null} value
   * @returns {string}
   */
  formatCurrency(value) {
    if (value === null || value === undefined || value === '') return '—';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  },

  /**
   * Escape a string for safe insertion into HTML.
   * @param {any} str
   * @returns {string}
   */
  escape(str) {
    if (str === null || str === undefined) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  },

  /** Convert a SNAKE_CASE string to Title Case. */
  titleCase(str) {
    if (!str) return '';
    return str.replace(/_/g, ' ').replace(/\w\S*/g, w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase());
  },

  /** Get today's date as YYYY-MM-DD for <input type="date"> default values. */
  todayISO() {
    return new Date().toISOString().slice(0, 10);
  },
};

const Render = {
  /**
   * Render a status badge for Purchase Order status values.
   * @param {string} status
   * @returns {string} HTML string
   */
  poBadge(status) {
    const map = {
      DRAFT:              'badge-draft',
      SENT:               'badge-sent',
      PARTIALLY_RECEIVED: 'badge-partially-received',
      RECEIVED:           'badge-received',
      CANCELLED:          'badge-cancelled',
    };
    const cls = map[status] || 'badge-cancelled';
    return `<span class="badge ${cls}">${Utils.escape(Utils.titleCase(status))}</span>`;
  },

  /**
   * Render a badge for Transaction type values.
   * @param {string} type
   * @returns {string} HTML string
   */
  txBadge(type) {
    const map = {
      PURCHASE:   'badge-purchase',
      SALE:       'badge-sale',
      ADJUSTMENT: 'badge-adjustment',
    };
    const cls = map[type] || 'badge-adjustment';
    return `<span class="badge ${cls}">${Utils.escape(Utils.titleCase(type))}</span>`;
  },

  /**
   * Render the empty-state panel inside a table card.
   * @param {string} message
   * @param {string} [icon]
   * @returns {string} HTML string
   */
  emptyState(message = 'No records found.', icon = '&#9636;') {
    return `
      <div class="empty-state">
        <div class="empty-state-icon">${icon}</div>
        <p class="empty-state-title">Nothing here yet</p>
        <p class="empty-state-body">${Utils.escape(message)}</p>
      </div>`;
  },

  /**
   * Wrap a <table> and optional caption inside a .table-card div.
   * @param {string} tableHtml
   * @returns {string}
   */
  tableCard(tableHtml) {
    return `<div class="table-card">${tableHtml}</div>`;
  },
};


const Modal = {
  _currentConfig: null,

  /**
   * Open the modal with a given configuration.
   * @param {{
   *   title: string,
   *   fields: Array<{name:string, label:string, type:string, required?:boolean,
   *                   options?:Array<{value,label}>, value?:any}>,
   *   submitLabel?: string,
   *   onSubmit: function(data:Object): Promise<void>
   * }} config
   */
  open(config) {
    Modal._currentConfig = config;

    const overlay    = document.getElementById('modal-overlay');
    const titleEl    = document.getElementById('modal-title');
    const bodyEl     = document.getElementById('modal-body');
    const submitBtn  = document.getElementById('modal-submit-btn');

    titleEl.textContent = config.title;
    submitBtn.textContent = config.submitLabel || 'Save';

    bodyEl.innerHTML = Modal._buildForm(config.fields);

    overlay.classList.remove('hidden');
    // Focus the first input for accessibility
    const firstInput = bodyEl.querySelector('input, select, textarea');
    if (firstInput) firstInput.focus();
  },

  /** Close the modal and reset state. */
  close() {
    const overlay = document.getElementById('modal-overlay');
    overlay.classList.add('hidden');
    Modal._currentConfig = null;
  },

  /**
   * Build the form HTML from a fields array.
   * @param {Array} fields
   * @returns {string}
   */
  _buildForm(fields) {
    return fields.map(field => {
      const required = field.required ? '<span class="required">*</span>' : '';
      let control = '';

      if (field.type === 'select') {
        const options = (field.options || [])
          .map(opt => {
            const selected = String(opt.value) === String(field.value ?? '') ? 'selected' : '';
            return `<option value="${Utils.escape(opt.value)}" ${selected}>${Utils.escape(opt.label)}</option>`;
          })
          .join('');
        control = `<select id="field-${field.name}" name="${field.name}" class="form-control"
                     ${field.required ? 'required' : ''}>
                     <option value="">— Select —</option>
                     ${options}
                   </select>`;
      } else if (field.type === 'textarea') {
        control = `<textarea id="field-${field.name}" name="${field.name}" class="form-control"
                     rows="3" placeholder="${Utils.escape(field.placeholder || '')}"
                     ${field.required ? 'required' : ''}>${Utils.escape(field.value ?? '')}</textarea>`;
      } else {
        control = `<input id="field-${field.name}" name="${field.name}" type="${field.type || 'text'}"
                     class="form-control" value="${Utils.escape(field.value ?? '')}"
                     placeholder="${Utils.escape(field.placeholder || '')}"
                     ${field.required ? 'required' : ''} />`;
      }

      return `
        <div class="form-group">
          <label class="form-label" for="field-${field.name}">${Utils.escape(field.label)}${required}</label>
          ${control}
          <span class="form-error" id="error-${field.name}"></span>
        </div>`;
    }).join('');
  },

  /**
   * Collect form values and validate required fields.
   * @returns {{data: Object|null, valid: boolean}}
   */
  _collectAndValidate() {
    const config = Modal._currentConfig;
    if (!config) return { data: null, valid: false };

    const data = {};
    let valid = true;

    for (const field of config.fields) {
      const el = document.getElementById(`field-${field.name}`);
      if (!el) continue;

      const errorEl = document.getElementById(`error-${field.name}`);
      const value = el.value.trim();

      // Clear previous error
      el.classList.remove('is-invalid');
      if (errorEl) { errorEl.textContent = ''; errorEl.classList.remove('visible'); }

      if (field.required && value === '') {
        el.classList.add('is-invalid');
        if (errorEl) { errorEl.textContent = `${field.label} is required.`; errorEl.classList.add('visible'); }
        valid = false;
        continue;
      }

      // Type coercion for numeric fields
      if ((field.type === 'number') && value !== '') {
        data[field.name] = parseFloat(value);
      } else {
        data[field.name] = value === '' ? null : value;
      }
    }

    return { data, valid };
  },

  /** Handle submit button click. */
  async _handleSubmit() {
    const { data, valid } = Modal._collectAndValidate();
    if (!valid) return;

    const submitBtn = document.getElementById('modal-submit-btn');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Saving…';

    try {
      await Modal._currentConfig.onSubmit(data);
      Modal.close();
    } catch (err) {
      Notify.error('Save failed', err.message);
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = Modal._currentConfig?.submitLabel || 'Save';
    }
  },
};

const Notify = {
  /**
   * Show a toast notification.
   * @param {'success'|'error'|'warning'} type
   * @param {string} title
   * @param {string} [message]
   */
  show(type, title, message = '') {
    const container = document.getElementById('toast-container');
    const icons = { success: '&#10003;', error: '&#10007;', warning: '&#9888;' };

    const toast = document.createElement('div');
    toast.className = `toast toast--${type}`;
    toast.innerHTML = `
      <span class="toast-icon" aria-hidden="true">${icons[type] || '&#9432;'}</span>
      <div class="toast-content">
        <p class="toast-title">${Utils.escape(title)}</p>
        ${message ? `<p class="toast-message">${Utils.escape(message)}</p>` : ''}
      </div>`;

    container.appendChild(toast);

    // Auto-dismiss
    setTimeout(() => Notify._dismiss(toast), Config.TOAST_DURATION);
  },

  /** Animate out and remove a toast element. */
  _dismiss(toast) {
    toast.classList.add('dismissing');
    toast.addEventListener('animationend', () => toast.remove(), { once: true });
    // Fallback removal in case animationend doesn't fire
    setTimeout(() => toast.remove(), 500);
  },

  success: (title, msg) => Notify.show('success', title, msg),
  error:   (title, msg) => Notify.show('error',   title, msg),
  warning: (title, msg) => Notify.show('warning', title, msg),
};


const Spinner = {
  show() { document.getElementById('spinner-overlay').classList.remove('hidden'); },
  hide() { document.getElementById('spinner-overlay').classList.add('hidden'); },
};


const Views = {


  async dashboard() {
    const container = document.getElementById('view-container');
    container.innerHTML = `<div class="loading-state"><div class="spinner"></div><p>Loading dashboard…</p></div>`;

    let products = [], suppliers = [], orders = [], transactions = [];
    try {
      [products, suppliers, orders, transactions] = await Promise.all([
        API.getProducts(),
        API.getSuppliers(),
        API.getPurchaseOrders(),
        API.getTransactions(),
      ]);
    } catch (err) {
      Notify.error('Failed to load dashboard', err.message);
      container.innerHTML = Render.emptyState('Could not load dashboard data. Is the server running?', '&#9888;');
      return;
    }

    const openOrders = orders.filter(o => ['DRAFT', 'SENT', 'PARTIALLY_RECEIVED'].includes(o.status));

    // Build stat cards HTML
    const stats = `
      <div class="stat-grid">
        <div class="stat-card stat-card--blue">
          <p class="stat-card-label">Total Products</p>
          <p class="stat-card-value">${products.length}</p>
          <p class="stat-card-sub">Inventory items tracked</p>
        </div>
        <div class="stat-card stat-card--green">
          <p class="stat-card-label">Total Suppliers</p>
          <p class="stat-card-value">${suppliers.length}</p>
          <p class="stat-card-sub">Active vendor records</p>
        </div>
        <div class="stat-card stat-card--amber">
          <p class="stat-card-label">Open Purchase Orders</p>
          <p class="stat-card-value">${openOrders.length}</p>
          <p class="stat-card-sub">Draft, sent, or partial</p>
        </div>
        <div class="stat-card stat-card--orange">
          <p class="stat-card-label">Total Transactions</p>
          <p class="stat-card-value">${transactions.length}</p>
          <p class="stat-card-sub">Purchases, sales, adjustments</p>
        </div>
      </div>`;

    // Build recent transactions table (last 5)
    const recent = [...transactions].reverse().slice(0, 5);
    let recentHtml = '';
    if (recent.length === 0) {
      recentHtml = Render.emptyState('No transactions recorded yet.', '&#9654;');
    } else {
      const rows = recent.map(tx => `
        <tr>
          <td class="text-mono">#${Utils.escape(tx.id)}</td>
          <td>${Utils.escape(tx.product?.name || '—')}</td>
          <td class="text-right">${Utils.escape(tx.quantity)}</td>
          <td>${Render.txBadge(tx.type)}</td>
          <td class="text-secondary">${Utils.formatDate(tx.transactionDate)}</td>
        </tr>`).join('');

      recentHtml = `
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th><th>Product</th><th>Qty</th><th>Type</th><th>Date</th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>`;
    }

    container.innerHTML = `
      ${stats}
      <div class="section-header">
        <div>
          <h2 class="section-title">Recent Transactions</h2>
          <p class="section-subtitle">Showing the last ${recent.length} transaction${recent.length !== 1 ? 's' : ''}</p>
        </div>
      </div>
      ${Render.tableCard(recentHtml)}`;
  },


  async products() {
    const container = document.getElementById('view-container');
    container.innerHTML = `<div class="loading-state"><div class="spinner"></div><p>Loading products…</p></div>`;

    let products = [];
    try {
      products = await API.getProducts();
    } catch (err) {
      Notify.error('Failed to load products', err.message);
      container.innerHTML = Render.emptyState('Could not load products.', '&#9643;');
      return;
    }

    const openProductModal = (product = null) => {
      Modal.open({
        title: product ? 'Edit Product' : 'Add Product',
        submitLabel: product ? 'Update Product' : 'Create Product',
        fields: [
          { name: 'name',     label: 'Product Name', type: 'text',   required: true,  value: product?.name    ?? '' },
          { name: 'sku',      label: 'SKU',           type: 'text',   required: true,  value: product?.sku     ?? '', placeholder: 'e.g. PROD-001' },
          { name: 'price',    label: 'Price (USD)',    type: 'number', required: false, value: product?.price   ?? '' },
          { name: 'cost',     label: 'Cost (USD)',     type: 'number', required: false, value: product?.cost    ?? '' },
          { name: 'quantity', label: 'Quantity',       type: 'number', required: false, value: product?.quantity ?? '' },
        ],
        onSubmit: async (data) => {
          if (product) {
            await API.updateProduct(product.id, data);
            Notify.success('Product updated', `"${data.name}" has been saved.`);
          } else {
            await API.createProduct(data);
            Notify.success('Product created', `"${data.name}" has been added.`);
          }
          Views.products();
        },
      });
    };

    const deleteProduct = async (id, name) => {
      if (!confirm(`Delete product "${name}"? This cannot be undone.`)) return;
      try {
        Spinner.show();
        await API.deleteProduct(id);
        Notify.success('Product deleted', `"${name}" was removed.`);
        Views.products();
      } catch (err) {
        Notify.error('Delete failed', err.message);
      } finally {
        Spinner.hide();
      }
    };

    // Wire "Add Product" button
    const addBtn = document.getElementById('add-btn');
    addBtn.onclick = () => openProductModal();

    let tableHtml = '';
    if (products.length === 0) {
      tableHtml = Render.emptyState('No products found. Add your first product to get started.', '&#9643;');
    } else {
      const rows = products.map(p => `
        <tr>
          <td>${Utils.escape(p.name)}</td>
          <td class="text-mono">${Utils.escape(p.sku)}</td>
          <td class="text-right">${Utils.formatCurrency(p.price)}</td>
          <td class="text-right">${Utils.formatCurrency(p.cost)}</td>
          <td class="text-right">${p.quantity ?? '—'}</td>
          <td>
            <div class="action-cell">
              <button class="action-btn action-btn--primary" title="Edit product"
                      onclick='Views._editProduct(${JSON.stringify(p)})'>&#9998;</button>
              <button class="action-btn action-btn--danger" title="Delete product"
                      onclick="Views._deleteProduct(${p.id}, '${Utils.escape(p.name)}')">&#10005;</button>
            </div>
          </td>
        </tr>`).join('');

      tableHtml = `
        <table class="data-table">
          <thead>
            <tr>
              <th>Name</th><th>SKU</th><th>Price</th><th>Cost</th><th>Qty</th><th></th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>`;
    }

    container.innerHTML = `
      <div class="section-header">
        <div>
          <h2 class="section-title">Products</h2>
          <p class="section-subtitle">${products.length} record${products.length !== 1 ? 's' : ''}</p>
        </div>
      </div>
      ${Render.tableCard(tableHtml)}`;

    // Expose handlers to inline onclick attributes (scoped to avoid globals pollution)
    Views._editProduct   = (p) => openProductModal(p);
    Views._deleteProduct = (id, name) => deleteProduct(id, name);
  },


  async suppliers() {
    const container = document.getElementById('view-container');
    container.innerHTML = `<div class="loading-state"><div class="spinner"></div><p>Loading suppliers…</p></div>`;

    let suppliers = [];
    try {
      suppliers = await API.getSuppliers();
    } catch (err) {
      Notify.error('Failed to load suppliers', err.message);
      container.innerHTML = Render.emptyState('Could not load suppliers.', '&#9651;');
      return;
    }

    const openSupplierModal = (supplier = null) => {
      Modal.open({
        title: supplier ? 'Edit Supplier' : 'Add Supplier',
        submitLabel: supplier ? 'Update Supplier' : 'Create Supplier',
        fields: [
          { name: 'name',        label: 'Company Name',  type: 'text', required: true,  value: supplier?.name        ?? '' },
          { name: 'contactName', label: 'Contact Name',  type: 'text', required: false, value: supplier?.contactName ?? '', placeholder: 'Primary contact' },
        ],
        onSubmit: async (data) => {
          if (supplier) {
            await API.updateSupplier(supplier.id, data);
            Notify.success('Supplier updated', `"${data.name}" has been saved.`);
          } else {
            await API.createSupplier(data);
            Notify.success('Supplier created', `"${data.name}" has been added.`);
          }
          Views.suppliers();
        },
      });
    };

    const deleteSupplier = async (id, name) => {
      if (!confirm(`Delete supplier "${name}"? This cannot be undone.`)) return;
      try {
        Spinner.show();
        await API.deleteSupplier(id);
        Notify.success('Supplier deleted', `"${name}" was removed.`);
        Views.suppliers();
      } catch (err) {
        Notify.error('Delete failed', err.message);
      } finally {
        Spinner.hide();
      }
    };

    const addBtn = document.getElementById('add-btn');
    addBtn.onclick = () => openSupplierModal();

    let tableHtml = '';
    if (suppliers.length === 0) {
      tableHtml = Render.emptyState('No suppliers yet. Add a supplier to begin creating purchase orders.', '&#9651;');
    } else {
      const rows = suppliers.map(s => `
        <tr>
          <td>${Utils.escape(s.name)}</td>
          <td class="text-secondary">${Utils.escape(s.contactName || '—')}</td>
          <td>
            <div class="action-cell">
              <button class="action-btn action-btn--primary" title="Edit supplier"
                      onclick='Views._editSupplier(${JSON.stringify(s)})'>&#9998;</button>
              <button class="action-btn action-btn--danger" title="Delete supplier"
                      onclick="Views._deleteSupplier(${s.id}, '${Utils.escape(s.name)}')">&#10005;</button>
            </div>
          </td>
        </tr>`).join('');

      tableHtml = `
        <table class="data-table">
          <thead>
            <tr>
              <th>Company Name</th><th>Contact Name</th><th></th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>`;
    }

    container.innerHTML = `
      <div class="section-header">
        <div>
          <h2 class="section-title">Suppliers</h2>
          <p class="section-subtitle">${suppliers.length} record${suppliers.length !== 1 ? 's' : ''}</p>
        </div>
      </div>
      ${Render.tableCard(tableHtml)}`;

    Views._editSupplier   = (s) => openSupplierModal(s);
    Views._deleteSupplier = (id, name) => deleteSupplier(id, name);
  },


  async purchaseOrders(filterStatus = '') {
    const container = document.getElementById('view-container');
    container.innerHTML = `<div class="loading-state"><div class="spinner"></div><p>Loading purchase orders…</p></div>`;

    let orders = [], suppliers = [];
    try {
      const params = filterStatus ? { status: filterStatus } : null;
      [orders, suppliers] = await Promise.all([
        API.getPurchaseOrders(params),
        API.getSuppliers(),
      ]);
    } catch (err) {
      Notify.error('Failed to load purchase orders', err.message);
      container.innerHTML = Render.emptyState('Could not load purchase orders.', '&#9636;');
      return;
    }

    const supplierOptions = suppliers.map(s => ({ value: s.id, label: s.name }));

    const openPOModal = (po = null) => {
      Modal.open({
        title: po ? 'Edit Purchase Order' : 'New Purchase Order',
        submitLabel: po ? 'Update Order' : 'Create Order',
        fields: [
          {
            name: 'supplierId', label: 'Supplier', type: 'select', required: true,
            options: supplierOptions,
            value: po?.supplier?.id ?? '',
          },
          {
            name: 'orderDate',    label: 'Order Date',    type: 'date', required: false,
            value: po?.orderDate    ? po.orderDate.slice(0, 10)    : Utils.todayISO(),
          },
          {
            name: 'expectedDate', label: 'Expected Date', type: 'date', required: false,
            value: po?.expectedDate ? po.expectedDate.slice(0, 10) : '',
          },
          {
            name: 'status', label: 'Status', type: 'select', required: false,
            options: Config.PO_STATUSES.map(s => ({ value: s, label: Utils.titleCase(s) })),
            value: po?.status ?? 'DRAFT',
          },
        ],
        onSubmit: async (data) => {
          const body = {
            supplier:     { id: parseInt(data.supplierId, 10) },
            orderDate:    data.orderDate,
            expectedDate: data.expectedDate,
            status:       data.status || 'DRAFT',
          };
          if (po) {
            await API.updatePurchaseOrder(po.id, body);
            Notify.success('Purchase order updated', `PO #${po.id} has been saved.`);
          } else {
            await API.createPurchaseOrder(body);
            Notify.success('Purchase order created', 'New PO has been created.');
          }
          Views.purchaseOrders(filterStatus);
        },
      });
    };

    const openStatusModal = (po) => {
      Modal.open({
        title: `Change Status — PO #${po.id}`,
        submitLabel: 'Update Status',
        fields: [
          {
            name: 'status', label: 'New Status', type: 'select', required: true,
            options: Config.PO_STATUSES.map(s => ({ value: s, label: Utils.titleCase(s) })),
            value: po.status,
          },
        ],
        onSubmit: async (data) => {
          await API.patchPOStatus(po.id, data.status);
          Notify.success('Status updated', `PO #${po.id} is now ${Utils.titleCase(data.status)}.`);
          Views.purchaseOrders(filterStatus);
        },
      });
    };

    const deletePO = async (id) => {
      if (!confirm(`Delete Purchase Order #${id}? This cannot be undone.`)) return;
      try {
        Spinner.show();
        await API.deletePurchaseOrder(id);
        Notify.success('Purchase order deleted', `PO #${id} was removed.`);
        Views.purchaseOrders(filterStatus);
      } catch (err) {
        Notify.error('Delete failed', err.message);
      } finally {
        Spinner.hide();
      }
    };

    // Build filter bar
    const statusOpts = ['', ...Config.PO_STATUSES]
      .map(s => `<option value="${s}" ${s === filterStatus ? 'selected' : ''}>${s ? Utils.titleCase(s) : 'All Statuses'}</option>`)
      .join('');

    const filterBar = `
      <div class="filter-bar">
        <span class="filter-label">Filter by status:</span>
        <select class="filter-select" id="po-status-filter">${statusOpts}</select>
      </div>`;

    const addBtn = document.getElementById('add-btn');
    addBtn.onclick = () => openPOModal();

    let tableHtml = '';
    if (orders.length === 0) {
      tableHtml = Render.emptyState(
        filterStatus
          ? `No purchase orders with status "${Utils.titleCase(filterStatus)}".`
          : 'No purchase orders yet. Create your first PO to get started.',
        '&#9636;'
      );
    } else {
      const rows = orders.map(po => {
        const isDraft = po.status === 'DRAFT';
        const editBtn = isDraft
          ? `<button class="action-btn action-btn--primary" title="Edit order (DRAFT only)"
                     onclick='Views._editPO(${JSON.stringify(po)})'>&#9998;</button>`
          : `<button class="action-btn" title="Can only edit DRAFT orders" disabled style="opacity:0.35;cursor:not-allowed">&#9998;</button>`;
        const deleteBtn = isDraft
          ? `<button class="action-btn action-btn--danger" title="Delete order (DRAFT only)"
                     onclick="Views._deletePO(${po.id})">&#10005;</button>`
          : `<button class="action-btn" title="Can only delete DRAFT orders" disabled style="opacity:0.35;cursor:not-allowed">&#10005;</button>`;

        return `
          <tr>
            <td class="text-mono">#${Utils.escape(po.id)}</td>
            <td>${Utils.escape(po.supplier?.name || '—')}</td>
            <td class="text-secondary">${Utils.formatDate(po.orderDate)}</td>
            <td class="text-secondary">${Utils.formatDate(po.expectedDate)}</td>
            <td>${Render.poBadge(po.status)}</td>
            <td>
              <div class="action-cell">
                ${editBtn}
                <button class="action-btn action-btn--primary" title="Change status"
                        onclick='Views._changeStatus(${JSON.stringify(po)})'>&#8635;</button>
                ${deleteBtn}
              </div>
            </td>
          </tr>`;
      }).join('');

      tableHtml = `
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th><th>Supplier</th><th>Order Date</th><th>Expected Date</th><th>Status</th><th></th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>`;
    }

    container.innerHTML = `
      <div class="section-header">
        <div>
          <h2 class="section-title">Purchase Orders</h2>
          <p class="section-subtitle">${orders.length} order${orders.length !== 1 ? 's' : ''}${filterStatus ? ` matching "${Utils.titleCase(filterStatus)}"` : ''}</p>
        </div>
      </div>
      ${filterBar}
      ${Render.tableCard(tableHtml)}`;

    // Wire filter select
    document.getElementById('po-status-filter').addEventListener('change', (e) => {
      Views.purchaseOrders(e.target.value);
    });

    Views._editPO       = (po) => openPOModal(po);
    Views._changeStatus = (po) => openStatusModal(po);
    Views._deletePO     = (id) => deletePO(id);
  },


  async transactions() {
    const container = document.getElementById('view-container');
    container.innerHTML = `<div class="loading-state"><div class="spinner"></div><p>Loading transactions…</p></div>`;

    let transactions = [], products = [];
    try {
      [transactions, products] = await Promise.all([
        API.getTransactions(),
        API.getProducts(),
      ]);
    } catch (err) {
      Notify.error('Failed to load transactions', err.message);
      container.innerHTML = Render.emptyState('Could not load transactions.', '&#9654;');
      return;
    }

    const productOptions = products.map(p => ({ value: p.id, label: `${p.name} (${p.sku})` }));

    const openTxModal = () => {
      Modal.open({
        title: 'Log Transaction',
        submitLabel: 'Log Transaction',
        fields: [
          {
            name: 'productId', label: 'Product', type: 'select', required: true,
            options: productOptions,
          },
          { name: 'quantity', label: 'Quantity', type: 'number', required: true, placeholder: 'e.g. 50' },
          {
            name: 'type', label: 'Transaction Type', type: 'select', required: true,
            options: Config.TX_TYPES.map(t => ({ value: t, label: Utils.titleCase(t) })),
          },
        ],
        onSubmit: async (data) => {
          const body = {
            product:  { id: parseInt(data.productId, 10) },
            quantity: data.quantity,
            type:     data.type,
          };
          await API.createTransaction(body);
          Notify.success('Transaction logged', `${Utils.titleCase(data.type)} of ${data.quantity} units recorded.`);
          Views.transactions();
        },
      });
    };

    const addBtn = document.getElementById('add-btn');
    addBtn.onclick = () => openTxModal();

    // Show most recent first
    const sorted = [...transactions].reverse();

    let tableHtml = '';
    if (sorted.length === 0) {
      tableHtml = Render.emptyState('No transactions recorded yet. Log your first transaction to track inventory movement.', '&#9654;');
    } else {
      const rows = sorted.map(tx => `
        <tr>
          <td class="text-mono">#${Utils.escape(tx.id)}</td>
          <td>${Utils.escape(tx.product?.name || '—')}</td>
          <td class="text-right">${Utils.escape(tx.quantity)}</td>
          <td>${Render.txBadge(tx.type)}</td>
          <td class="text-secondary">${Utils.formatDate(tx.transactionDate)}</td>
        </tr>`).join('');

      tableHtml = `
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th><th>Product</th><th>Quantity</th><th>Type</th><th>Date</th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>`;
    }

    container.innerHTML = `
      <div class="section-header">
        <div>
          <h2 class="section-title">Transactions</h2>
          <p class="section-subtitle">${sorted.length} record${sorted.length !== 1 ? 's' : ''} &mdash; append-only ledger</p>
        </div>
      </div>
      ${Render.tableCard(tableHtml)}`;
  },
};


const Navigation = {
  /** View configuration: maps data-view attribute to display metadata. */
  _views: {
    dashboard:       { title: 'Dashboard',        addLabel: null },
    products:        { title: 'Products',          addLabel: '+ Add Product' },
    suppliers:       { title: 'Suppliers',         addLabel: '+ Add Supplier' },
    'purchase-orders': { title: 'Purchase Orders', addLabel: '+ New Purchase Order' },
    transactions:    { title: 'Transactions',      addLabel: '+ Log Transaction' },
  },

  /**
   * Navigate to a named view, updating the DOM and calling the render fn.
   * @param {string} viewName
   */
  go(viewName) {
    const meta = Navigation._views[viewName];
    if (!meta) return;

    // Update sidebar active state
    document.querySelectorAll('.nav-item').forEach(item => {
      item.classList.toggle('active', item.dataset.view === viewName);
    });

    // Update topbar
    document.getElementById('page-title').textContent = meta.title;
    document.getElementById('breadcrumb-current').textContent = meta.title;

    // Show/hide and label the add button
    const addBtn = document.getElementById('add-btn');
    if (meta.addLabel) {
      addBtn.classList.remove('hidden');
      document.getElementById('add-btn-label').textContent = meta.addLabel;
    } else {
      addBtn.classList.add('hidden');
    }

    // Render the view
    switch (viewName) {
      case 'dashboard':       Views.dashboard();       break;
      case 'products':        Views.products();        break;
      case 'suppliers':       Views.suppliers();       break;
      case 'purchase-orders': Views.purchaseOrders();  break;
      case 'transactions':    Views.transactions();    break;
    }
  },

  /** Bind all sidebar nav items to Navigation.go(). */
  init() {
    document.querySelectorAll('.nav-item[data-view]').forEach(item => {
      item.addEventListener('click', () => Navigation.go(item.dataset.view));
    });
  },
};


const initModal = () => {
  document.getElementById('modal-close-btn').addEventListener('click',  Modal.close);
  document.getElementById('modal-cancel-btn').addEventListener('click', Modal.close);
  document.getElementById('modal-submit-btn').addEventListener('click', Modal._handleSubmit);

  // Close when clicking backdrop
  document.getElementById('modal-overlay').addEventListener('click', (e) => {
    if (e.target === e.currentTarget) Modal.close();
  });

  // Close on Escape key
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') Modal.close();
  });
};

document.addEventListener('DOMContentLoaded', () => {
  Navigation.init();
  initModal();
  Navigation.go('dashboard'); // Start on the dashboard
});