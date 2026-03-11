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
      <v-col cols="3" v-if="tab === 'transactions'">
        <v-select
          v-model="selectedTagFilter"
          :items="tagFilterItems"
          label="태그 필터"
          clearable
          density="compact"
          hide-details
        />
      </v-col>
    </v-row>

    <!-- 기간 필터 (거래 내역 탭일 때만) -->
    <v-row v-if="tab === 'transactions'" class="mb-4" align="center">
      <v-col cols="2">
        <v-text-field
          v-model="startDate"
          label="시작일"
          type="date"
          density="compact"
          hide-details
        />
      </v-col>
      <v-col cols="auto" class="px-0 text-center">~</v-col>
      <v-col cols="2">
        <v-text-field
          v-model="endDate"
          label="종료일"
          type="date"
          density="compact"
          hide-details
        />
      </v-col>
      <v-col cols="auto">
        <v-btn color="primary" variant="outlined" @click="loadTransactions">조회</v-btn>
      </v-col>
      <v-spacer />
      <v-col cols="auto">
        <v-chip color="primary" variant="tonal" size="large">
          합계: {{ formatAmount(totalAmount) }} ({{ filteredTransactions.length }}건)
        </v-chip>
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
        <template #item.amount="{ item, value }">
          <span class="text-right d-block" :class="{ 'text-decoration-line-through text-grey': item.cancelled }">
            {{ formatAmount(value) }}
          </span>
          <v-chip v-if="item.cancelled" color="red" size="x-small" variant="flat" class="ml-1">취소</v-chip>
        </template>
        <template #item.transactionDate="{ value }">
          {{ formatDate(value) }}
        </template>
        <template #item.tags="{ item }">
          <v-chip
            v-if="item.mainTag"
            :color="item.mainTag.color"
            size="x-small"
            variant="flat"
            class="text-white mr-1"
          >
            ★ {{ item.mainTag.name }}
          </v-chip>
          <v-chip
            v-for="tag in item.detailTags"
            :key="tag.id"
            :color="tag.color"
            size="x-small"
            variant="outlined"
            class="mr-1"
          >
            {{ tag.name }}
          </v-chip>
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
    <v-dialog v-model="transactionDialog" max-width="550">
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

          <!-- 주요 태그 -->
          <v-divider class="my-3" />
          <div class="text-subtitle-2 mb-2">주요 태그</div>
          <div class="d-flex flex-wrap ga-1 mb-2">
            <v-chip
              v-if="selectedTransaction.mainTag"
              :color="selectedTransaction.mainTag.color"
              size="small"
              variant="flat"
              class="text-white"
              closable
              @click:close="removeTag(selectedTransaction.id, selectedTransaction.mainTag.id)"
            >
              ★ {{ selectedTransaction.mainTag.name }}
            </v-chip>
            <span v-else class="text-grey text-body-2">미지정</span>
          </div>
          <v-select
            v-model="mainTagToSet"
            :items="mainTagOptions"
            label="주요 태그 설정"
            density="compact"
            hide-details
            clearable
            @update:model-value="changeMainTag"
          />

          <!-- 세부 태그 -->
          <div class="text-subtitle-2 mt-4 mb-2">세부 태그</div>
          <div class="d-flex flex-wrap ga-1 mb-2">
            <v-chip
              v-for="tag in selectedTransaction.detailTags"
              :key="tag.id"
              :color="tag.color"
              size="small"
              variant="outlined"
              closable
              @click:close="removeTag(selectedTransaction.id, tag.id)"
            >
              {{ tag.name }}
            </v-chip>
            <span v-if="!selectedTransaction.detailTags || selectedTransaction.detailTags.length === 0" class="text-grey text-body-2">
              세부 태그 없음
            </span>
          </div>
          <v-select
            v-model="tagToAdd"
            :items="availableTagsForSelected"
            label="세부 태그 추가"
            density="compact"
            hide-details
            clearable
            @update:model-value="addTag"
          />
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

    <v-snackbar v-model="snackbar.show" :color="snackbar.color" timeout="3000">
      {{ snackbar.text }}
    </v-snackbar>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchTransactions, fetchParseFailures, fetchCreditCards,
  fetchTags, addTagToTransaction, removeTagFromTransaction, setMainTag
} from '../api'

const route = useRoute()
const router = useRouter()

const tab = ref('transactions')
const selectedCard = ref(null)
const selectedTagFilter = ref(null)
const loading = ref(false)
const transactions = ref([])
const failures = ref([])
const creditCards = ref([])
const allTags = ref([])
const transactionDialog = ref(false)
const failureDialog = ref(false)
const selectedTransaction = ref(null)
const selectedFailure = ref(null)
const tagToAdd = ref(null)
const mainTagToSet = ref(null)
const snackbar = ref({ show: false, text: '', color: '' })

/** 기간 필터: 기본값은 이번 달 1일 ~ 말일 */
const now = new Date()
const startDate = ref(new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10))
const endDate = ref(new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().slice(0, 10))

const transactionHeaders = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '카드사', key: 'cardCompanyName' },
  { title: '카드번호', key: 'cardLastFourDigits', width: 100 },
  { title: '금액', key: 'amount', align: 'end' },
  { title: '사용처', key: 'merchantName' },
  { title: '태그', key: 'tags', sortable: false, width: 200 },
  { title: '거래일시', key: 'transactionDate' },
]

const failureHeaders = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '카드사', key: 'cardCompanyName' },
  { title: '실패 사유', key: 'failReason' },
  { title: '원본 메시지', key: 'rawMessage' },
  { title: '수신일시', key: 'createdAt' },
]

/** 등록된 카드 기반 필터 항목 */
const cardItems = computed(() =>
  creditCards.value.map(c => ({
    title: `${c.cardCompanyName} (${c.lastFourDigits})`,
    value: c.id
  }))
)

/** 태그 필터 항목 */
const tagFilterItems = computed(() =>
  allTags.value.map(t => ({
    title: t.name,
    value: t.id
  }))
)

/** 필터링된 거래의 사용금액 총합 (취소 건 제외) */
const totalAmount = computed(() =>
  filteredTransactions.value
    .filter(tx => !tx.cancelled)
    .reduce((sum, tx) => sum + Number(tx.amount), 0)
)

/** 카드별·태그별 필터링된 거래 */
const filteredTransactions = computed(() => {
  let result = transactions.value

  if (selectedCard.value) {
    const card = creditCards.value.find(c => c.id === selectedCard.value)
    if (card) {
      result = result.filter(t =>
        t.cardCompany === card.cardCompany && t.cardLastFourDigits === card.lastFourDigits
      )
    }
  }

  if (selectedTagFilter.value) {
    result = result.filter(t =>
      t.tags && t.tags.some(tag => tag.id === selectedTagFilter.value)
    )
  }

  return result
})

/** MAIN 유형 태그 목록 */
const mainTagList = computed(() => allTags.value.filter(t => t.tagType === 'MAIN'))

/** DETAIL 유형 태그 목록 */
const detailTagList = computed(() => allTags.value.filter(t => t.tagType === 'DETAIL'))

/** "태그 생성" 이동 항목 값 */
const CREATE_TAG_VALUE = '__create_tag__'

/** 선택된 거래에 아직 할당되지 않은 DETAIL 태그 (세부 태그 추가용) + "태그 생성" 항목 */
const availableTagsForSelected = computed(() => {
  if (!selectedTransaction.value) return []
  const assignedIds = (selectedTransaction.value.tags || []).map(t => t.id)
  const items = detailTagList.value
    .filter(t => !assignedIds.includes(t.id))
    .map(t => ({ title: t.name, value: t.id }))
  items.push({ title: '+ 태그 생성', value: CREATE_TAG_VALUE })
  return items
})

/** 주요 태그 설정용 옵션 (MAIN 유형 태그만) + "태그 생성" 항목 */
const mainTagOptions = computed(() => {
  if (!selectedTransaction.value) return []
  const mainTagId = selectedTransaction.value.mainTag?.id
  const items = mainTagList.value
    .filter(t => t.id !== mainTagId)
    .map(t => ({ title: t.name, value: t.id }))
  items.push({ title: '+ 태그 생성', value: CREATE_TAG_VALUE })
  return items
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

/** 거래에 세부 태그 추가 */
async function addTag(tagId) {
  if (!tagId || !selectedTransaction.value) return
  // "태그 생성" 선택 시 태그 관리 페이지로 이동
  if (tagId === CREATE_TAG_VALUE) {
    tagToAdd.value = null
    router.push('/tags')
    return
  }
  try {
    const res = await addTagToTransaction(selectedTransaction.value.id, tagId)
    // 로컬 상태 업데이트
    const tagData = res.data
    if (tagData) {
      if (!selectedTransaction.value.tags) selectedTransaction.value.tags = []
      if (!selectedTransaction.value.detailTags) selectedTransaction.value.detailTags = []
      selectedTransaction.value.tags.push(tagData)
      selectedTransaction.value.detailTags.push(tagData)
    }
    tagToAdd.value = null
    snackbar.value = { show: true, text: '세부 태그 할당 완료', color: 'green' }
  } catch (e) {
    snackbar.value = { show: true, text: '태그 할당 실패: ' + (e.response?.data?.message || e.message), color: 'red' }
  }
}

/** 주요 태그 변경 */
async function changeMainTag(tagId) {
  if (!tagId || !selectedTransaction.value) return
  // "태그 생성" 선택 시 태그 관리 페이지로 이동
  if (tagId === CREATE_TAG_VALUE) {
    mainTagToSet.value = null
    router.push('/tags')
    return
  }
  try {
    await setMainTag(selectedTransaction.value.id, tagId)
    // 거래 데이터 새로고침
    await refreshSelectedTransaction()
    mainTagToSet.value = null
    snackbar.value = { show: true, text: '주요 태그 설정 완료', color: 'green' }
  } catch (e) {
    snackbar.value = { show: true, text: '주요 태그 설정 실패: ' + (e.response?.data?.message || e.message), color: 'red' }
  }
}

/** 거래에서 태그 제거 */
async function removeTag(transactionId, tagId) {
  try {
    await removeTagFromTransaction(transactionId, tagId)
    // 거래 데이터 새로고침
    await refreshSelectedTransaction()
    snackbar.value = { show: true, text: '태그 해제 완료', color: 'green' }
  } catch (e) {
    snackbar.value = { show: true, text: '태그 해제 실패', color: 'red' }
  }
}

/** 선택된 거래의 태그 정보 새로고침 */
async function refreshSelectedTransaction() {
  if (!selectedTransaction.value) return
  const txId = selectedTransaction.value.id
  // 전체 목록에서도 업데이트
  await loadTransactions()
  const updated = transactions.value.find(t => t.id === txId)
  if (updated) {
    selectedTransaction.value = updated
  }
}

async function loadTransactions() {
  loading.value = true
  try {
    const res = await fetchTransactions(startDate.value, endDate.value)
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
  const [cardsRes, tagsRes] = await Promise.all([
    fetchCreditCards(),
    fetchTags()
  ])
  creditCards.value = cardsRes.data
  allTags.value = tagsRes.data

  if (route.query.tab === 'failures') {
    tab.value = 'failures'
  }
  if (route.query.card) {
    selectedCard.value = Number(route.query.card)
  }

  load()
})

watch(tab, load)
</script>
