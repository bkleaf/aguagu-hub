<template>
  <div>
    <!-- 필터 -->
    <v-row class="mb-4" align="center">
      <v-col cols="auto">
        <v-btn-toggle v-model="tab" mandatory color="primary" density="comfortable">
          <v-btn value="transactions">거래 내역</v-btn>
          <v-btn value="failures">파싱 실패</v-btn>
        </v-btn-toggle>
      </v-col>
      <v-col cols="3" v-if="tab === 'transactions'">
        <v-select
          v-model="selectedCard"
          :items="cardItems"
          label="카드 필터"
          clearable
          density="compact"
          hide-details
        />
      </v-col>
    </v-row>

    <!-- 거래 목록 테이블 -->
    <v-card v-if="tab === 'transactions'">
      <v-data-table
        :headers="transactionHeaders"
        :items="filteredTransactions"
        :loading="loading"
        items-per-page="15"
        @click:row="openTransactionDetail"
        hover
      >
        <template #item.amount="{ value }">
          <span class="text-right d-block">{{ formatAmount(value) }}</span>
        </template>
        <template #item.transactionDate="{ value }">
          {{ formatDate(value) }}
        </template>
      </v-data-table>
    </v-card>

    <!-- 파싱 실패 목록 테이블 -->
    <v-card v-if="tab === 'failures'">
      <v-data-table
        :headers="failureHeaders"
        :items="failures"
        :loading="loading"
        items-per-page="15"
        @click:row="openFailureDetail"
        hover
      >
        <template #item.rawMessage="{ value }">
          <span class="text-truncate d-inline-block" style="max-width: 300px;">{{ value }}</span>
        </template>
        <template #item.createdAt="{ value }">
          {{ formatDate(value) }}
        </template>
      </v-data-table>
    </v-card>

    <!-- 거래 상세 다이얼로그 -->
    <v-dialog v-model="transactionDialog" max-width="500">
      <v-card v-if="selectedTransaction">
        <v-card-title>거래 상세 #{{ selectedTransaction.id }}</v-card-title>
        <v-card-text>
          <v-list density="compact">
            <v-list-item title="카드사" :subtitle="selectedTransaction.cardCompanyName" />
            <v-list-item title="카드번호" :subtitle="selectedTransaction.cardLastFourDigits ? '****' + selectedTransaction.cardLastFourDigits : '-'" />
            <v-list-item title="금액" :subtitle="formatAmount(selectedTransaction.amount)" />
            <v-list-item title="사용처" :subtitle="selectedTransaction.merchantName || '-'" />
            <v-list-item title="거래일시" :subtitle="formatDate(selectedTransaction.transactionDate)" />
            <v-list-item title="누적금액" :subtitle="selectedTransaction.accumulatedAmount ? formatAmount(selectedTransaction.accumulatedAmount) : '-'" />
            <v-list-item title="전화번호" :subtitle="selectedTransaction.phoneNumber || '-'" />
          </v-list>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="transactionDialog = false">닫기</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 파싱 실패 상세 다이얼로그 -->
    <v-dialog v-model="failureDialog" max-width="600">
      <v-card v-if="selectedFailure">
        <v-card-title>파싱 실패 #{{ selectedFailure.id }}</v-card-title>
        <v-card-text>
          <v-list density="compact">
            <v-list-item title="카드사" :subtitle="selectedFailure.cardCompanyName" />
            <v-list-item title="실패 사유" :subtitle="selectedFailure.failReason" />
            <v-list-item title="수신일시" :subtitle="formatDate(selectedFailure.createdAt)" />
            <v-list-item title="전화번호" :subtitle="selectedFailure.phoneNumber || '-'" />
          </v-list>
          <v-divider class="my-2" />
          <div class="text-subtitle-2 mb-1">원본 메시지</div>
          <pre class="text-body-2 bg-grey-lighten-4 pa-2 rounded" style="white-space: pre-wrap;">{{ selectedFailure.rawMessage }}</pre>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="failureDialog = false">닫기</v-btn>
        </v-card-actions>
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

const transactionHeaders = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '카드사', key: 'cardCompanyName' },
  { title: '카드번호', key: 'cardLastFourDigits', width: 100 },
  { title: '금액', key: 'amount', align: 'end' },
  { title: '사용처', key: 'merchantName' },
  { title: '거래일시', key: 'transactionDate' },
]

const failureHeaders = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '카드사', key: 'cardCompanyName' },
  { title: '실패 사유', key: 'failReason' },
  { title: '원본 메시지', key: 'rawMessage' },
  { title: '수신일시', key: 'createdAt' },
]

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

const formatAmount = (v) => Number(v).toLocaleString('ko-KR') + '원'
const formatDate = (v) => v ? v.replace('T', ' ').slice(0, 16) : '-'

const openTransactionDetail = (_, { item }) => {
  selectedTransaction.value = item
  transactionDialog.value = true
}

const openFailureDetail = (_, { item }) => {
  selectedFailure.value = item
  failureDialog.value = true
}

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
