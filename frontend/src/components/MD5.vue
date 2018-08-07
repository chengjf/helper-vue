<template>
  <div>
    <input type="text" v-model="password" placeholder="password">
    <input type="text" v-model="count" placeholder="count">

    <button @click="submit()">Submit</button>

    <div><h6>Result: {{ result }}</h6></div>

  </div>
</template>

<script>
  import {AXIOS} from './http-common'

  export default {
    name: "MD5",
    data() {
      return {
        errors: [],
        password: "",
        count: "",
        result: ""
      }
    },
    methods: {
      // Fetches posts when the component is created.
      submit() {
        console.log("password:{} count:{}", this.password, this.count)
        console.log(this.password + " " + this.count)
        // var params = new URLSearchParams()
        // params.append('password', this.password)
        // params.append('count', this.count)

        var params = {
          "password": this.password,
          "count": this.count
      }

        AXIOS.get(`/md5`, {"params": params})
          .then(response => {
            // JSON responses are automatically parsed.
            this.result = response.data
            console.log(response.data)
          })
          .catch(e => {
            this.errors.push(e)
          })
      }
    }
  }
</script>

<style scoped>

</style>
