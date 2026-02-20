<template>
  <!-- 상단 앱바 — 현재 페이지 제목 표시 -->
  <v-app-bar density="compact" color="primary" flat>
    <v-app-bar-title class="text-body-1 font-weight-bold">
      {{ currentTitle }}
    </v-app-bar-title>
  </v-app-bar>

  <!-- 페이지 콘텐츠 -->
  <v-main>
    <v-container class="pa-3">
      <router-view />
    </v-container>
  </v-main>

  <!-- 하단 네비게이션 바 -->
  <v-bottom-navigation v-model="activeNav" grow color="primary" class="mobile-nav">
    <v-btn value="dashboard" @click="$router.push('/')">
      <v-icon>mdi-view-dashboard</v-icon>
      <span class="text-caption">대시보드</span>
    </v-btn>
    <v-btn value="transactions" @click="$router.push('/transactions')">
      <v-icon>mdi-receipt-text</v-icon>
      <span class="text-caption">거래</span>
    </v-btn>
    <v-btn value="limits" @click="$router.push('/limits')">
      <v-icon>mdi-chart-bar</v-icon>
      <span class="text-caption">한도</span>
    </v-btn>
    <v-btn value="monthlyBills" @click="$router.push('/monthly-bills')">
      <v-icon>mdi-file-document-outline</v-icon>
      <span class="text-caption">청구서</span>
    </v-btn>
    <v-btn value="more" @click="$router.push('/more')">
      <v-icon>mdi-dots-horizontal</v-icon>
      <span class="text-caption">더보기</span>
    </v-btn>
  </v-bottom-navigation>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeNav = ref('dashboard')

/** 현재 라우트에 따라 하단 네비게이션 활성 상태 동기화 */
const navMap = {
  dashboard: 'dashboard',
  transactions: 'transactions',
  transactionCreate: 'transactions',
  limits: 'limits',
  monthlyBills: 'monthlyBills',
  more: 'more',
  creditCards: 'more',
  autoPayments: 'more',
}

watch(() => route.name, (name) => {
  activeNav.value = navMap[name] || 'dashboard'
}, { immediate: true })

/** 현재 페이지 제목 */
const currentTitle = computed(() => route.meta?.title || 'aguagu-hub')
</script>

<style scoped>
/* 하단 네비게이션 버튼의 최소 너비를 줄여 좁은 화면에서 잘리지 않도록 함 */
.mobile-nav :deep(.v-btn) {
  min-width: 56px !important;
}
</style>
