package generic;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import generic.Operand.OperandType;
import java.util.*;
import java.nio.*;

public class Simulator {
		
	static FileInputStream inputcodeStream = null;
	
	public static void setupSimulation(String assemblyProgramFile)
	{	
		int firstCodeAddress = ParsedProgram.parseDataSection(assemblyProgramFile);
		ParsedProgram.parseCodeSection(assemblyProgramFile, firstCodeAddress);
		ParsedProgram.printState();
	}

	private static String binary_FixedLength(int n,int l){
		int temp=n;

		if(n<0){
			temp*=-1;
		}

		String a="";
		while(temp>0){
			a=String.valueOf(temp%2)+a;
			temp/=2;
		}

		String b="";
		if(n<0){
			a="1"+a;
			b=String.format("%"+l+"s",a).replace(' ','1');
		}
		else{
			b=String.format("%"+l+"s",a).replace(' ','0');
		}

		return b;
	}

	private static String operand_to_string(Operand a, int l){
		if(a==null){
			return binary_FixedLength(0, l);
		}

		else if(a.getOperandType()==Operand.OperandType.Label){
			return binary_FixedLength(ParsedProgram.symtab.get(a.getLabelValue()), l);
		}

		else{
			return binary_FixedLength(a.getValue(),l);
		}
	}

	private static Map<Instruction.OperationType, String> table = new HashMap<Instruction.OperationType, String>();

	public static Instruction.OperationType[] operands = {
		Instruction.OperationType.add,
		Instruction.OperationType.addi,
		Instruction.OperationType.sub,
		Instruction.OperationType.subi,
		Instruction.OperationType.mul,
		Instruction.OperationType.muli,
		Instruction.OperationType.div,
		Instruction.OperationType.divi,
		Instruction.OperationType.and,
		Instruction.OperationType.andi,
		Instruction.OperationType.or,
		Instruction.OperationType.ori,
		Instruction.OperationType.xor,
		Instruction.OperationType.xori,
		Instruction.OperationType.slt,
		Instruction.OperationType.slti,
		Instruction.OperationType.sll,
		Instruction.OperationType.slli,
		Instruction.OperationType.srl,
		Instruction.OperationType.srli,
		Instruction.OperationType.sra,
		Instruction.OperationType.srai,
		Instruction.OperationType.load,
		Instruction.OperationType.end,
		Instruction.OperationType.beq,
		Instruction.OperationType.jmp,
		Instruction.OperationType.bne,
		Instruction.OperationType.blt,
		Instruction.OperationType.bgt,
		Instruction.OperationType.store
	};

	public static String[] operands_code={
		"00000",
		"00001",
		"00010",
		"00011",
		"00100",
		"00101",
		"00110",
		"00111",
		"01000",
		"01001",
		"01010",
		"01011",
		"01100",
		"01101",
		"01110",
		"01111",
		"10000",
		"10001",
		"10010",
		"10011",
		"10100",
		"10101",
		"10110",
		"11101",
		"11001",
		"11000",
		"11010",
		"11011",
		"11100",
		"10111"
};

	public static void make_table(){
		for(int i=0;i<30;i++){
			table.put(operands[i],operands_code[i]);
		}
	}
	
	public static void assemble(String objectProgramFile)
	{
		//TODO your assembler code

		make_table();

		FileOutputStream output_file;
		try{

		//1. open the objectProgramFile in binary mode

		output_file=new FileOutputStream(objectProgramFile);
		BufferedOutputStream buffer =new BufferedOutputStream(output_file);


		//2. write the firstCodeAddress to the file

		byte[] add_firstCodeAddress = ByteBuffer.allocate(4).putInt(ParsedProgram.firstCodeAddress).array();
		buffer.write(add_firstCodeAddress);


		//3. write the data to the file

		for(int d:ParsedProgram.data){
			byte[] new_data=ByteBuffer.allocate(4).putInt(d).array();
			buffer.write(new_data);
		}


		//4. assemble one instruction at a time, and write to the file

		for(Instruction i: ParsedProgram.code){
			String encode="";

			String op_code=table.get(i.getOperationType());
			encode+=op_code;
			int int_opcode=Integer.parseInt(op_code,2);
			int programCounter=i.getProgramCounter();

			if(int_opcode<=20 && int_opcode%2==0){
				String s1,s2,dO,empty;
				s1=operand_to_string(i.getSourceOperand1(), 5);
				s2=operand_to_string(i.getSourceOperand2(), 5);
				dO=operand_to_string(i.getDestinationOperand(), 5);
				empty=binary_FixedLength(0, 12);
				encode+=s1+s2+dO+empty;
			}

			else if(int_opcode<=21 && int_opcode%2==1){
				String s1,dO,s2;
				s1=operand_to_string(i.getSourceOperand1(), 5);
				dO=operand_to_string(i.getDestinationOperand(), 5);
				s2=operand_to_string(i.getSourceOperand2(), 17);
				encode+=s1+dO+s2;
			}

			else if(int_opcode==22 || int_opcode==23){
				String s1,dO,s2;
				s1=operand_to_string(i.getSourceOperand1(), 5);
				dO=operand_to_string(i.getDestinationOperand(), 5);
				s2=operand_to_string(i.getSourceOperand2(), 17);
				encode+=s1+dO+s2;
			}

			else if( int_opcode==24 ){
				String empty,dOjump;
				empty=binary_FixedLength(0, 5);
				int temp=Integer.parseInt(operand_to_string(i.getDestinationOperand(), 5) ,2)-programCounter;
				String btemp=binary_FixedLength(temp, 22);
				dOjump=btemp.substring(btemp.length()-22);
				encode+=empty+dOjump;
			}

			else if( int_opcode>=25 && int_opcode<=28 ){
				String s1,s2,dOjump;
				s1=operand_to_string(i.getSourceOperand1(), 5);
				s2=operand_to_string(i.getSourceOperand2(), 5);
				int temp=Integer.parseInt(operand_to_string(i.getDestinationOperand(), 5) ,2)-programCounter;
				String btemp=binary_FixedLength(temp, 17);
				dOjump=btemp.substring(btemp.length()-17);
				encode+=s1+s2+dOjump;
			}

			else if( int_opcode==29 ){
				String empty;
				empty=binary_FixedLength(0, 27);
				encode+=empty;
			}

			int integer_Rep=(int) Long.parseLong(encode,2);
			byte[] binary_Rep=ByteBuffer.allocate(4).putInt(integer_Rep).array();
			buffer.write(binary_Rep);


		}

		//5. close the file

		buffer.close();
		output_file.close();
		}

		catch(Exception except){
			except.printStackTrace();
		}
	}
	
}
