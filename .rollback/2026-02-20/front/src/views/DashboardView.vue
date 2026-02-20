<template>
  <div>
    <!-- 요약 카드 -->
    <v-row class="mb-4">
      <v-col cols="12" md="4">
        <v-card @click="$router.push('/transactions')" style="cursor: pointer" hover>
          <v-card-text class="text-center">
            <div class="text-h4 text-green">{{ stats.transactions }}</div>
            <div class="text-subtitle-1 text-grey">거래 건수</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card @click="$router.push('/transactions?tab=failures')" style="cursor: pointer" hover>
          <v-card-text class="text-center">
            <div class="text-h4 text-red">{{ stats.failures }}</div>
            <div class="text-subtitle-1 text-grey">파싱 실패</div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card @click="$router.push('/credit-cards')" style="cursor: pointer" hover>
          <v-card-text class="text-center">
            <div class="text-h4 text-blue">{{ stats.companies }}</div>
            <div class="text-subtitle-1 text-grey">지원 카드사</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- 전체 합산 사용 금액 -->
    <v-card class="mb-4" v-if="summary" @click="$router.push('/transactions')" style="cursor: pointer" hover>
      <v-card-text class="text-center">
        <div class="text-subtitle-2 text-grey">전체 사용액</div>
        <div class="text-h5 font-weight-bold">{{ formatAmount(summary.totalUsedAmount) }}</div>
      </v-card-text>
    </v-card>

    <!-- 카드별 사용 현황 -->
    <div class="text-h6 mb-3">카드별 사용 현황</div>
    <v-row v-if="summary && summary.cards.length > 0">
      <v-col v-for="card in summary.cards" :key="card.creditCardId" cols="12" md="6">
        <v-card
          @click="$router.push('/transactions?card=' + card.creditCardId)"
          style="cursor: pointer"
          hover
        >
          <v-card-title class="d-flex align-center justify-space-between">
            <span>{{ card.cardCompanyName }} ({{ card.lastFourDigits }})</span>
            <v-chip size="small" variant="outlined" color="grey">
              {{ formatPeriod(card.periodStart, card.periodEnd) }}
            </v-chip>
          </v-card-title>
          <v-card-text>
            <!-- 한도 설정된 카드 -->
            <template v-if="card.hasLimit">
              <div class="d-flex justify-space-between mb-1">
                <span class="text-body-2 text-grey">
                  {{ card.limitTypeName }} 한도 사용액
                </span>
                <span class="text-body-2 font-weight-bold">
                  {{ formatAmount(card.limitUsedAmount) }} / {{ formatAmount(card.limitAmount) }}
                </span>
              </div>
              <v-progress-linear
                :model-value="Math.min(card.usagePercent, 100)"
                :color="getProgressColor(card.usagePercent)"
                height="12"
                rounded
                class="mb-2"
              >
                <template #default>
                  <span class="text-caption font-weight-bold">{{ card.usagePercent.toFixed(1) }}%</span>
                </template>
              </v-progress-linear>
              <div class="d-flex justify-space-between">
                <span class="text-caption text-grey">
                  정산기간 사용액 {{ formatAmount(card.usedAmount) }}
                </span>
                <span class="text-caption" :class="card.remaining >= 0 ? 'text-green' : 'text-red'">
                  잔여 {{ formatAmount(card.remaining) }}
                </span>
              </div>
            </template>

            <!-- 한도 미설정 카드 -->
            <template v-else>
              <div class="d-flex justify-space-between mb-1">
                <span class="text-body-2 text-grey">사용액</span>
                <span class="text-body-2 font-weight-bold">{{ formatAmount(card.usedAmount) }}</span>
              </div>
              <v-progress-linear
                :model-value="0"
                color="grey-lighten-2"
                height="12"
                rounded
                class="mb-2"
              ></v-progress-linear>
              <div class="text-right">
                <span class="text-caption text-grey-lighten-1">한도 미설정</span>
              </div>
            </template>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- 활성 카드 없음 -->
    <v-card v-else-if="summary && summary.cards.length === 0">
      <v-card-text class="text-center text-grey pa-8">
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
