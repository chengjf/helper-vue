import Vue from 'vue'
import Router from 'vue-router'
// import Hello from '@/components/Hello'
import Service from '@/components/Service'
import Bootstrap from '@/components/Bootstrap'
import User from '@/components/User'
import MD5 from '@/components/MD5'
import Json from '@/components/Json'
import Youtube from '@/components/Youtube'
import Default from '@/components/Default'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'Default',
      component: Default
    },
    {
      path: '/callservice',
      name: 'Service',
      component: Service
    },
    {
      path: '/bootstrap',
      name: 'Bootstrap',
      component: Bootstrap
    },
    {
      path: '/user',
      name: 'User',
      component: User
    },
    {
      path: '/md5',
      name: 'MD5',
      component: MD5
    },
    {
      path: '/json',
      name: 'Json',
      component: Json
    },
    {
      path: '/youtube',
      name: 'Youtube',
      component: Youtube
    }
  ]
})
