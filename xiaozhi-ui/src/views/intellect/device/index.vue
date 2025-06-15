<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" >
      <el-form-item label="设备Mac" prop="macAddress">
        <el-input
          v-model="queryParams.macAddress"
          placeholder="请输入设备Mac地址"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="ClientId" prop="clientId">
        <el-input
          v-model="queryParams.clientId"
          placeholder="请输入客户端ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="在线状态" clearable style="width: 180px">
          <el-option
              v-for="dict in device_online_status"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="MagicStick"
          @click="handleActivation"
          v-hasPermi="['intellect:device:activation']"
        >激活</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['intellect:device:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['intellect:device:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['intellect:device:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="deviceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="设备Mac" align="center" prop="macAddress" min-width="140" />
      <el-table-column label="ClientId" align="center" prop="clientId" min-width="150" show-overflow-tooltip />
      <el-table-column label="设备状态" align="center" prop="status" min-width="80">
        <template #default="scope">
          <dict-tag :options="device_online_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="智能体" align="center" prop="agentName" min-width="100" show-overflow-tooltip/>
      <el-table-column label="设备备注" align="center" prop="remarks" min-width="100" show-overflow-tooltip/>
      <el-table-column label="称呼" align="center" prop="username" min-width="80" show-overflow-tooltip/>
      <el-table-column label="连接时间" align="center" prop="lastConnAt" min-width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.lastConnAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="导入时间" align="center" prop="createTime" min-width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" min-width="200">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['intellect:device:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['intellect:device:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改设备管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="deviceRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="称呼" prop="username">
          <el-input
              v-model="form.username"
              placeholder="请输入称呼"
              maxlength="10"
              show-word-limit
              prefix-icon="Document"
          />
        </el-form-item>
        <el-form-item label="智能体" prop="agentId">
          <el-select v-model="form.agentId" placeholder="请选择智能体">
            <el-option
                v-for="agent in agentList"
                :key="agent.id"
                :label="agent.agentName"
                :value="agent.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remarks">
          <el-input
              v-model="form.remarks"
              placeholder="请输入设备备注"
              maxlength="32"
              show-word-limit
              type="textarea"
              rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 设备激活对话框  -->
    <el-dialog title="设备激活" v-model="activateOpen" width="500px" append-to-body>
      <el-form ref="activateRef" :model="activateForm" :rules="activateRules">
        <el-form-item label="激活验证码" prop="code">
          <el-input
              prefix-icon="CircleCheck"
              v-model="activateForm.code"
              placeholder="请输入验证码"
              maxlength="6"
              show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="activateSubmit">确 定</el-button>
          <el-button @click="activateCancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Device">
import { listDevice, getDevice, delDevice, addDevice, updateDevice, activationDevice } from "@/api/intellect/device"
import { agentListAll } from '@/api/intellect/agent.js';

const { proxy } = getCurrentInstance()
const { device_online_status } = proxy.useDict("device_online_status")

const deviceList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
// 智能体下拉列表
const agentList = ref([])
agentListAll().then(res => agentList.value = res.data);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    macAddress: null,
    clientId: null,
    lastConnAt: null,
    status: null,
    agentId: null,
    remarks: null,
  },
  rules: {
    agentId: { required: true, message: "智能体不能为空", trigger: "blur" },
    username: { required: true, message: "称呼不能为空", trigger: "blur" },
    remarks: { required: true, message: "设备备注不能为空", trigger: "blur" }
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 激活对话框参数 */
const {activateOpen, activateForm, activateRules} = toRefs(reactive({
  activateOpen: false,
  activateForm: {
    code: null
  },
  activateRules: {
    code: [
        { required: true, message: "激活码不能为空", trigger: "blur" }
    ]
  }
}))

/** 查询设备管理列表 */
function getList() {
  loading.value = true
  listDevice(queryParams.value).then(response => {
    deviceList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    agentId: null,
    remarks: null,
    username: null
  }
  proxy.resetForm("deviceRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加设备管理"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getDevice(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改设备"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["deviceRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateDevice(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addDevice(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除设备管理编号为"' + _ids + '"的数据项？').then(function() {
    return delDevice(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('intellect/device/export', {
    ...queryParams.value
  }, `device_${new Date().getTime()}.xlsx`)
}

/** 激活按钮操作 */
function handleActivation() {
  activateForm.value.code = null;
  activateOpen.value = true
}

function activateCancel() {
  activateOpen.value = false
  activateForm.value.code = null;
}

function activateSubmit() {
  proxy.$refs["activateRef"].validate(valid => {
    if (!valid) {
      return;
    }
    activationDevice(activateForm.value.code).then(response => {
      if (response.code === 200) {
        proxy.$modal.msgSuccess("激活成功")
      }else {
        proxy.$modal.msgError(response.msg)
      }
      activateOpen.value = false;
      getList();
    })
  })
}

getList()
</script>
