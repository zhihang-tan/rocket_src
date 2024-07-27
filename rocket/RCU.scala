/*runahead code begin*/
//==========================================================
package freechips.rocketchip.rocket
import chisel3._
import chisel3.experimental.BaseModule

//==========================================================
// Parameters
//==========================================================
case class RCU_Params (
  xLen: Int = 64
)

//==========================================================
// I/Os
//==========================================================
class RCU_IO (params: RCU_Params) extends Bundle {
  val wb_valid = Input(Bool())
  val l2miss = Input(Bool())
  val rf_in = Input(Vec(31,UInt(params.xLen.W)))
  val sb_in = Input(Vec(31,UInt(1.W)))
  val ipc = Input(UInt(40.W))
  val rf_out = Output(Vec(31,UInt(params.xLen.W)))
  val sb_out = Output(Vec(31,UInt(1.W)))
  val runahead_backflag = Output(Bool())
  val runahead_trig = Output(Bool())
  val runahead_flag = Output(Bool())
  val opc = Output(UInt(40.W))

}

trait Has_RCU_IO extends BaseModule {
  val params: RCU_Params
  val io = IO(new RCU_IO(params))
}

//==========================================================
// Implementations
//==========================================================
class RCU (val params: RCU_Params) extends Module with Has_RCU_IO {
  val rf_reg = RegInit(VecInit(Seq.fill(31)(0.U(params.xLen.W))))
  val sb_reg = RegInit(VecInit(Seq.fill(31)(0.U(1.W))))
  val storepc = RegInit(0.U(40.W))
  //initialize
  for (i <- 0 until 31) {
    io.rf_out(i) := 0.U(params.xLen.W)
  }
  for (i <- 0 until 31) {
    io.sb_out(i) := 0.U(1.W)
  }
  io.runahead_backflag := false.B
  io.runahead_trig := false.B
  io.runahead_flag := false.B
  val rh_flag = RegInit(0.U(1.W))
  io.runahead_flag := rh_flag
  dontTouch(rh_flag)
  dontTouch(io)
  dontTouch(rf_reg)
  dontTouch(sb_reg)

  when(io.l2miss) {
    for (i <- 0 until 31) {
      io.runahead_trig := true.B
      rh_flag := 1.U
      rf_reg(i) := io.rf_in(i)
      sb_reg(i) := io.sb_in(i)
    }
    storepc := io.ipc
  }
  io.opc := storepc

  when(io.wb_valid) {
    for (j <- 0 until 31) {
      io.runahead_backflag := true.B
      rh_flag := 0.U
      io.rf_out(j) := rf_reg(j)
      io.sb_out(j) := sb_reg(j)
    }
  } 
}
/*runahead code end*/
