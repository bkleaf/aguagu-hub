import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import TransactionListView from '../views/TransactionListView.vue'
import TransactionCreateView from '../views/TransactionCreateView.vue'
import LimitListView from '../views/LimitListView.vue'
import CreditCardListView from '../views/CreditCardListView.vue'
import MoreView from '../views/MoreView.vue'
import AutoPaymentListView from '../views/AutoPaymentListView.vue'
import MonthlyBillListView from '../views/MonthlyBillListView.vue'
import TagListView from '../views/TagListView.vue'
import TagStatisticsView from '../views/TagStatisticsView.vue'
import TagAmountSummaryView from '../views/TagAmountSummaryView.vue'

const routes = [
  { path: '/', name: 'dashboard', component: DashboardView, meta: { title: '대시보드' } },
  { path: '/transactions', name: 'transactions', component: TransactionListView, meta: { title: '거래 내역' } },
  { path: '/transactions/new', name: 'transactionCreate', component: TransactionCreateView, meta: { title: '문자 등록' } },
  { path: '/limits', name: 'limits', component: LimitListView, meta: { title: '카드 한도' } },
  { path: '/credit-cards', name: 'creditCards', component: CreditCardListView, meta: { title: '카드 관리' } },
  { path: '/more', name: 'more', component: MoreView, meta: { title: '더보기' } },
  { path: '/auto-payments', name: 'autoPayments', component: AutoPaymentListView, meta: { title: '자동결제' } },
  { path: '/monthly-bills', name: 'monthlyBills', component: MonthlyBillListView, meta: { title: '월별 청구서' } },
  { path: '/tags', name: 'tags', component: TagListView, meta: { title: '태그 관리' } },
  { path: '/tag-statistics', name: 'tagStatistics', component: TagStatisticsView, meta: { title: '태그 통계' } },
  { path: '/tag-amount-summary', name: 'tagAmountSummary', component: TagAmountSummaryView, meta: { title: '태그 금액 합산' } },
]

const router = createRouter({
  history: createWebHistory('/m/'),
  routes,
})

export default router
