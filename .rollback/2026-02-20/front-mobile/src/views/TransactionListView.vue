<template>
  <div>
    <!-- 탭 전환 -->
    <v-btn-toggle v-model="tab" mandatory color="primary" density="compact" class="mb-3" style="width: 100%">
      <v-btn value="transactions" style="flex: 1">거래 내역</v-btn>
      <v-btn value="failures" style="flex: 1">파싱 실패</v-btn>
    </v-btn-toggle>

    <!-- 카드 필터 -->
    <v-select
      v-if="tab === 'transactions'"
      v-model="selectedCard"
      :items="cardItems"
      label="카드 필터"
      clearable
      density="compact"
      hide-details
      class="mb-3"
    />

    <!-- 로딩 -->
    <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-2" />

    <!-- 거래 카드 리스트 -->
    <template v-if="tab === 'transactions'">
      <v-card
        v-for="tx in filteredTransactions"
        :key="tx.id"
        class="mb-2"
        variant="outlined"
        @click="openTransactionDetail(tx)"
      >
        <v-card-text class="py-2">
          <div class="d-flex justify-space-between align-center">
            <div style="min-width: 0; flex: 1">
              <div class="text-body-2 font-weight-medium text-truncate">{{ tx.merchantName || '사용처 없음' }}</div>
              <div class="text-caption text-grey">{{ tx.cardCompanyName }} &middot; {{ formatDate(tx.transactionDate) }}</div>
            </div>
            <div class="text-body-1 font-weight-bold ml-2 text-no-wrap">{{ formatAmount(tx.amount) }}</div>
          </div>
        </v-card-text>
      </v-card>

      <v-card v-if="!loading && filteredTransactions.length === 0" variant="outlined">
        <v-card-text class="text-center text-grey py-6">거래 내역이 없습니다</v-card-text>
      </v-card>
    </template>

    <!-- 파싱 실패 카드 리스트 -->
    <template v-if="tab === 'failures'">
      <v-card
        v-for="f in failures"
        :key="f.id"
        class="mb-2"
        variant="outlined"
        @click="openFailureDetail(f)"
      >
        <v-card-text class="py-2">
          <div class="text-body-2 font-weight-medium">{{ f.cardCompanyName }}</div>
          <div class="text-caption text-grey mb-1">{{ formatDate(f.createdAt) }}</div>
          <div class="text-caption text-red">{{ f.failReason }}</div>
        </v-card-text>
      </v-card>

      <v-card v-if="!loading && failures.length === 0" variant="outlined">
        <v-card-text class="text-center text-grey py-6">파싱 실패 내역이 없습니다</v-card-text>
      </v-card>
    </template>

    <!-- 거래 상세 다이얼로그 (풀스크린) -->
    <v-dialog v-model="transactionDialog" fullscreen transition="dialog-bottom-transition">
      <v-card v-if="selectedTransaction">
        <v-toolbar color="primary" density="compact">
          <v-btn icon="mdi-close" @click="transactionDialog = false" />
          <v-toolbar-title class="text-body-1">거래 상세 #{{ selectedTransaction.id }}</v-toolbar-title>
        </v-toolbar>
        <v-card-text>
          <v-list>
            <v-list-item title="카드사" :subtitle="selectedTransaction.cardCompanyName" />
            <v-list-item title="카드번호" :subtitle="selectedTransaction.cardLastFourDigits ? '****' + selectedTransaction.cardLastFourDigits : '-'" />
            <v-list-item title="금액" :subtitle="formatAmount(selectedTransaction.amount)" />
            <v-list-item title="사용처" :subtitle="selectedTransaction.merchantName || '-'" />
            <v-list-item title="거래일시" :subtitle="formatDate(selectedTransaction.transactionDate)" />
            <v-list-item title="누적금액" :subtitle="selectedTransaction.accumulatedAmount ? formatAmount(selectedTransaction.accumulatedAmount) : '-'" />
            <v-list-item title="전화번호" :subtitle="selectedTransaction.phoneNumber || '-'" />
          </v-list>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- 파싱 실패 상세 다이얼로그 (풀스크린) -->
    <v-dialog v-model="failureDialog" fullscreen transition="dialog-bottom-transition">
      <v-card v-if="selectedFailure">
        <v-toolbar color="primary" density="compact">
          <v-btn icon="mdi-close" @click="failureDialog = false" />
          <v-toolbar-title class="text-body-1">파싱 실패 #{{ selectedFailure.id }}</v-toolbar-title>
        </v-toolbar>
        <v-card-text>
          <v-list>
            <v-list-item title="카드사" :subtitle="selectedFailure.cardCompanyName" />
            <v-list-item title="실패 사유" :subtitle="selectedFailure.failReason" />
            <v-list-item title="수신일시" :subtitle="formatDate(selectedFailure.createdAt)" />
            <v-list-item title="전화번호" :subtitle="selectedFailure.phoneNumber || '-'" />
          </v-list>
          <v-divider class="my-2" />
          <div class="text-subtitle-2 mb-1">원본 메시지</div>
          <pre class="text-body-2 bg-grey-lighten-4 pa-2 rounded" style="white-space: pre-wrap; word-break: break-all;">{{ selectedFailure.rawMessage }}</pre>
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchTransactions, fetchParseFailures, fetchCreditCards } from '../api'

const route = useRoute()

const tab = ref('transactions')
const selectedCard = ref(null)
const loading = ref(false)
const transactions = ref([])
const failures = ref([])
const creditCards = ref([])
const transactionDialog = ref(false)
const failureDialog = ref(false)
const selectedTransaction = ref(null)
const selectedFailure = ref(null)

/** 등록된 카드 기반 필터 항목 (카드사명 + 끝4자리) */
const cardItems = computed(() =>
  creditCards.value.map(c => ({
    title: `${c.cardCompanyName} (${c.lastFourDigits})`,
    value: c.id
  }))
)

/** 카드별 필터링된 거래 */
const filteredTransactions = computed(() => {
  if (!selectedCard.value) return transactions.value
  const card = creditCards.value.find(c => c.id === selectedCard.value)
  if (!card) return transactions.value
  return transactions.value.filter(t =>
    t.cardCompany === card.cardCompany && t.cardLastFourDigits === card.lastFourDigits
  )
})

/** 금액 포맷 */
const formatAmount = (v) => Number(v).toLocaleString('ko-KR') + '원'
/** 날짜 포맷 */
const formatDate = (v) => v ? v.replace('T', ' ').slice(0, 16) : '-'

/** 거래 상세 열기 */
function openTransactionDetail(tx) {
  selectedTransaction.value = tx
  transactionDialog.value = true
}

/** 파싱 실패 상세 열기 */
function openFailureDetail(f) {
  selectedFailure.value = f
  failureDialog.value = true
}

/** 거래 목록 로드 */
async function loadTransactions() {
  loading.value = true
  try {
    const res = await fetchTransactions()
    transactions.value = res.data
  } catch (e) {
    console.error('거래 목록 로딩 실패', e)
  } finally {
    loading.value = false
  }
}

/** 파싱 실패 목록 로드 */
async function loadFailures() {
  loading.value = true
  try {
    const res = await fetchParseFailures()
    failures.value = res.data
  } catch (e) {
    console.error('파싱 실패 목록 로딩 실패', e)
  } finally {
    loading.value = false
  }
}

/** 탭에 따라 데이터 로드 */
async function load() {
  if (tab.value === 'transactions') {
    await loadTransactions()
  } else {
    await loadFailures()
  }
}

onMounted(async () => {
  const res = await fetchCreditCards()
  creditCards.value = res.data

  // 쿼리 파라미터로 초기 탭/필터 설정
  if (route.query.tab === 'failures') {
    tab.value = 'failures'
  }
  if (route.query.card) {
    selectedCard.value = route.query.card
  }

  load()
})

watch(tab, load)
</script>
