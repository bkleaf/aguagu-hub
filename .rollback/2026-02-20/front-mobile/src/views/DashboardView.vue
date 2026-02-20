<template>
  <div>
    <!-- 요약 카드 (세로 배치) -->
    <v-card class="mb-3" variant="outlined" @click="$router.push('/transactions')" style="cursor: pointer">
      <v-card-text class="d-flex align-center justify-space-between">
        <div>
          <div class="text-caption text-grey">거래 건수</div>
          <div class="text-h5 font-weight-bold text-green">{{ stats.transactions }}</div>
        </div>
        <v-icon size="40" color="green-lighten-2">mdi-receipt-text</v-icon>
      </v-card-text>
    </v-card>

    <v-card class="mb-3" variant="outlined" @click="$router.push('/transactions?tab=failures')" style="cursor: pointer">
      <v-card-text class="d-flex align-center justify-space-between">
        <div>
          <div class="text-caption text-grey">파싱 실패</div>
          <div class="text-h5 font-weight-bold text-red">{{ stats.failures }}</div>
        </div>
        <v-icon size="40" color="red-lighten-2">mdi-alert-circle</v-icon>
      </v-card-text>
    </v-card>

    <v-card class="mb-3" variant="outlined" @click="$router.push('/credit-cards')" style="cursor: pointer">
      <v-card-text class="d-flex align-center justify-space-between">
        <div>
          <div class="text-caption text-grey">지원 카드사</div>
          <div class="text-h5 font-weight-bold text-blue">{{ stats.companies }}</div>
        </div>
        <v-icon size="40" color="blue-lighten-2">mdi-credit-card-multiple</v-icon>
      </v-card-text>
    </v-card>

    <!-- 전체 합산 사용 금액 -->
    <v-card class="mb-3" variant="outlined" v-if="summary" @click="$router.push('/transactions')" style="cursor: pointer">
      <v-card-text class="text-center">
        <div class="text-caption text-grey">전체 사용액</div>
        <div class="text-h5 font-weight-bold">{{ formatAmount(summary.totalUsedAmount) }}</div>
      </v-card-text>
    </v-card>

    <!-- 카드별 사용 현황 -->
    <div class="text-subtitle-1 font-weight-bold mb-2">카드별 사용 현황</div>

    <template v-if="summary && summary.cards.length > 0">
      <v-card
        v-for="card in summary.cards"
        :key="card.creditCardId"
        class="mb-2"
        variant="outlined"
        @click="$router.push('/transactions?card=' + card.creditCardId)"
        style="cursor: pointer"
      >
        <v-card-text class="py-3">
          <!-- 카드사명 + 정산기간 -->
          <div class="d-flex justify-space-between align-center mb-2">
            <span class="text-body-2 font-weight-medium">
              {{ card.cardCompanyName }} ({{ card.lastFourDigits }})
            </span>
            <v-chip size="x-small" variant="outlined" color="grey">
              {{ formatPeriod(card.periodStart, card.periodEnd) }}
            </v-chip>
          </div>

          <!-- 한도 설정된 카드 -->
          <template v-if="card.hasLimit">
            <div class="d-flex justify-space-between mb-1">
              <span class="text-caption text-grey">{{ card.limitTypeName }} 한도 사용액</span>
              <span class="text-caption font-weight-bold">
                {{ formatAmount(card.limitUsedAmount) }} / {{ formatAmount(card.limitAmount) }}
              </span>
            </div>
            <v-progress-linear
              :model-value="Math.min(card.usagePercent, 100)"
              :color="getProgressColor(card.usagePercent)"
              height="10"
              rounded
              class="mb-1"
            >
              <template #default>
                <span class="text-caption font-weight-bold" style="font-size: 10px">{{ card.usagePercent.toFixed(1) }}%</span>
              </template>
            </v-progress-linear>
            <div class="d-flex justify-space-between">
              <span class="text-caption text-grey">
                정산기간 {{ formatAmount(card.usedAmount) }}
              </span>
              <span class="text-caption" :class="card.remaining >= 0 ? 'text-green' : 'text-red'">
                잔여 {{ formatAmount(card.remaining) }}
              </span>
            </div>
          </template>

          <!-- 한도 미설정 카드 -->
          <template v-else>
            <div class="d-flex justify-space-between mb-1">
              <span class="text-caption text-grey">사용액</span>
              <span class="text-caption font-weight-bold">{{ formatAmount(card.usedAmount) }}</span>
            </div>
            <v-progress-linear
              :model-value="0"
              color="grey-lighten-2"
              height="10"
              rounded
              class="mb-1"
            ></v-progress-linear>
            <div class="text-right">
              <span class="text-caption text-grey-lighten-1">한도 미설정</span>
            </div>
          </template>
        </v-card-text>
      </v-card>
    </template>

    <v-card v-else-if="summary && summary.cards.length === 0" variant="outlined">
      <v-card-text class="text-center text-grey py-6">
        등록된 활성 카드가 없습니다
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchTransactionsByDate, fetchParseFailures, fetchSupportedCompanies, fetchUsageSummary } from '../api'

const stats = ref({ transactions: 0, failures: 0, companies: 0 })
const summary = ref(null)

/** 금액 포맷 */
const formatAmount = (v) => Number(v).toLocaleString('ko-KR') + '원'

/** 정산기간을 표시 형식으로 변환합니다. (예: 2/1 ~ 2/28) */
function formatPeriod(start, end) {
  const s = new Date(start)
  const e = new Date(end)
  return `${s.getMonth() + 1}/${s.getDate()} ~ ${e.getMonth() + 1}/${e.getDate()}`
}

/** 사용률에 따른 프로그레스바 색상을 반환합니다. */
function getProgressColor(percent) {
  if (percent >= 80) return 'red'
  if (percent >= 50) return 'orange'
  return 'green'
}

onMounted(async () => {
  try {
    const [transactionsRes, failuresRes, companiesRes, summaryRes] = await Promise.all([
      fetchTransactionsByDate(),
      fetchParseFailures(),
      fetchSupportedCompanies(),
      fetchUsageSummary(),
    ])

    stats.value = {
      transactions: transactionsRes.data.length,
      failures: failuresRes.data.length,
      companies: companiesRes.data.length,
    }
    summary.value = summaryRes.data
  } catch (e) {
    console.error('대시보드 로딩 실패', e)
  }
})
</script>
