import chisel3._
import Constants._
import util._
class BusInterface(coreId : Int) extends Module{
  val io = IO(new Bundle{
    val grant   = Input(Bool())
    val reqOut  = Output(Bool())
    val tx      = Output(UInt(GLOBALADDRWIDTH.W))
    val rx      = Input(UInt(GLOBALADDRWIDTH.W))
    
    val spikeID = Input(UInt(GLOBALADDRWIDTH.W))
    val axonID  = Output(UInt(AXONIDWIDTH.W))
    val valid   = Output(Bool())
    val ack     = Output(Bool())
    val reqIn   = Input(Bool())
  })

  io.tx := 0.U
  when(io.grant){
    io.tx := io.spikeID 
  }

  io.reqOut := io.reqIn && ~io.grant
  io.ack := io.grant

  //TODO this is a temporary ROM do a function for mapping actual network 
  val romContent = (0 until CORES).map(i=>if (i % log2Up(CORES) == 0) (1 << AXONMSBWIDTH) | (i >> log2Up(CORES)-AXONMSBWIDTH) else (i >> log2Up(CORES)-AXONMSBWIDTH))
  val romContChi = romContent.map(i => i.asUInt((AXONMSBWIDTH+1).W))
  val synROMReg  = RegInit(0.U((AXONMSBWIDTH+1).W))
  val filterROM  = VecInit(romContChi)
  //TODO this is a temporary ROM do a function for mapping actual network 
  
  val enaROM     = Wire(Bool())
  val axonIDLSB  = RegInit(0.U((AXONIDWIDTH - AXONMSBWIDTH).W))

  enaROM := io.rx.orR //or reduction
  axonIDLSB := io.rx(AXONIDWIDTH -AXONMSBWIDTH - 1, 0)

  synROMReg := 0.U //default assigment to dataport
  when(enaROM){
    synROMReg := filterROM(io.rx(GLOBALADDRWIDTH-1, GLOBALADDRWIDTH - log2Up(CORES)))
  }

  io.valid := synROMReg(AXONMSBWIDTH)
  io.axonID := synROMReg(AXONMSBWIDTH-1, 0) ## axonIDLSB
}