import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1/card',
})

// 거래 내역 API
export const fetchTransactions = () => api.get('/transactions')
export const fetchTransaction = (id) => api.get(`/transactions/${id}`)
export const fetchTransactionsByDate = () => api.get('/transactions/by-date')
export const fetchTransactionsByCompany = (company) => api.get(`/transactions/by-company/${company}`)
export const fetchSupportedCompanies = () => api.get('/transactions/supported-companies')
export const createTransaction = (data) => api.post('/transactions', data)

// 파싱 실패 내역 API
export const fetchParseFailures = () => api.get('/transactions/parse-failures')

// 카드 한도 API
export const fetchLimits = () => api.get('/limits')
export const fetchLimit = (id) => api.get(`/limits/${id}`)
export const createOrUpdateLimit = (data) => api.post('/limits', data)
export const deleteLimit = (id) => api.delete(`/limits/${id}`)

// 월별 청구서 API
export const fetchMonthlyBills = (year, month, cardCompany) => {
  const params = { year, month }
  if (cardCompany) params.cardCompany = cardCompany
  return api.get('/monthly-bills', { params })
}
export const fetchAllMonthlyBills = () => api.get('/monthly-bills/all')

// 신용카드 API
export const fetchCreditCards = () => api.get('/credit-cards')
export const fetchCreditCard = (id) => api.get(`/credit-cards/${id}`)
export const registerCreditCard = (data) => api.post('/credit-cards', data)
export const deleteCreditCard = (id) => api.delete(`/credit-cards/${id}`)
export const fetchUsageSummary = () => api.get('/credit-cards/usage-summary')

// 자동결제 API
export const fetchAutoPayments = () => api.get('/auto-payments')
export const fetchAutoPayment = (id) => api.get(`/auto-payments/${id}`)
export const fetchAutoPaymentsByCard = (creditCardId) => api.get(`/auto-payments/by-card/${creditCardId}`)
export const createAutoPayment = (data) => api.post('/auto-payments', data)
export const updateAutoPayment = (id, data) => api.put(`/auto-payments/${id}`, data)
export const deleteAutoPayment = (id) => api.delete(`/auto-payments/${id}`)
