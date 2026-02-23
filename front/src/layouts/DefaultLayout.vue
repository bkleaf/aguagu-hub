<template>
  <v-navigation-drawer permanent>
    <v-list-item title="Aguagu-Hub" subtitle="Admin" class="pa-4" />
    <v-divider />
    <v-list nav density="compact">
      <v-list-item
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        :prepend-icon="item.icon"
        :title="item.title"
      />
    </v-list>
  </v-navigation-drawer>

  <v-app-bar flat border>
    <v-app-bar-title>{{ currentTitle }}</v-app-bar-title>
  </v-app-bar>

  <v-main>
    <v-container fluid>
      <router-view />
    </v-container>
  </v-main>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const currentTitle = computed(() => route.meta?.title || '대시보드')

const navItems = [
  { to: '/', icon: 'mdi-view-dashboard', title: '대시보드' },
  { to: '/transactions', icon: 'mdi-credit-card', title: '거래 내역' },
  { to: '/transactions/new', icon: 'mdi-message-plus', title: '문자 등록' },
  { to: '/monthly-bills', icon: 'mdi-file-document-outline', title: '월별 청구서' },
  { to: '/credit-cards', icon: 'mdi-card-account-details', title: '신용카드 관리' },
  { to: '/limits', icon: 'mdi-chart-bar', title: '카드 한도 관리' },
  { to: '/auto-payments', icon: 'mdi-refresh-auto', title: '자동결제 관리' },
  { to: '/tags', icon: 'mdi-tag-multiple', title: '태그 관리' },
  { to: '/tag-statistics', icon: 'mdi-chart-donut', title: '태그 통계' },
  { to: '/tag-amount-summary', icon: 'mdi-calculator', title: '태그 금액 합산' },
]
</script>
