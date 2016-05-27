/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */


//extension support file for the unalign encoder
//rsuvorov
#include "stdio.h"
#include "stdlib.h"

#include "per_codec/per_support.h"
#include "per_codec/per_ext_sup.h"
//макрос который позволяет выводить дебажную информацию на консоль
//#define DEBUG 

#define MAX_SIZE 32
//структура кольцевого буффера 
typedef struct shift_buf_s
{
	uint8_t buffer[2*MAX_SIZE+2];
	int32_t first_size;
	int32_t second_size;
	bool wpos_flag;
	bool rpos_flag;
}shift_buf_t;

//записать данные из start в buf с номером ячейки pos_flag
//asn_per_data_t указатель на позицию в потоке
//задача данной функции считать size бит  из потока ext_buf и поместить в buf
bool write_data(shift_buf_t *buf,asn_per_data_t *ext_buf,int32_t size)
{
	int32_t offset=0;
	//PRE:
	if(ext_buf==0) return false;
	if(size/8>MAX_SIZE) return false;
	//OPR:
	if(buf->wpos_flag==true)
	{
		buf->second_size=size;
		offset=MAX_SIZE;
		buf->wpos_flag=false;
	}
	else
	{
		buf->first_size=size;
		buf->wpos_flag=true;
	}

	if(per_get_many_bits(ext_buf, buf->buffer+offset,0,size)==-1)return false;
#ifdef DEBUG	
	printf("write swap buffer %d size=%d\n",buf->wpos_flag,size);
	for(int i=0; i<32; i++)
	{
		printf("%x ",buf->buffer[i+offset]);
		if(i==16)printf("\n");
	}
	printf("\n");
#endif //DEBUF
	return true;
}
//извлечь данные из buf с ячейкой pos_flag в ext_buf
int32_t read_data(shift_buf_t *buf,asn_per_outp_t *ext_buf)
{
	int32_t size=0;
	int32_t offset=0;
	int32_t ret=0;
	//PRE:
	if (ext_buf==0) return 0;
	//OPR:
	if(buf->rpos_flag==false) 
	{
		size=buf->first_size;
		buf->rpos_flag=true;
	}
	else 
	{
		size=buf->second_size;
		buf->rpos_flag=false;
	}

	if(buf->rpos_flag==false) offset=MAX_SIZE;
	ret=per_put_many_bits(ext_buf, buf->buffer+offset, size);
#ifdef DEBUG	
	printf("read data from %d size= %d\n",buf->rpos_flag,size); 
	for(uint8_t i=0; i<32; i++)
	{
		printf("%x ",ext_buf->tmpspace[i]);
		if(i==16)printf("\n");
	}
	printf("\n");
#endif //DEBUF
	return ret;
}
#define BYTE_SIZE 8

//данная функция выполняет нормализацию дескриптора потока
void buf_normalize(asn_per_outp_t *write_po)
{
	uint32_t complete_bytes;
	//Execute Normalization
	if(write_po->nboff >= 8) //check byte align 
	{//arrange shift params
		write_po->buffer += (write_po->nboff >> 3);
		write_po->nbits  -= (write_po->nboff & ~0x07);
		write_po->nboff  &= 0x07;
	}

	complete_bytes = (write_po->buffer - write_po->tmpspace);

	if(write_po->outper(&(write_po->tmpspace), complete_bytes, write_po->op_key) < 0)  printf("ERRROR\n");

	if(write_po->nboff)  write_po->tmpspace[0] = write_po->buffer[0]; //add rest bits 
	write_po->buffer = write_po->tmpspace;//init point to start buffer
	write_po->nbits = BYTE_SIZE * sizeof(write_po->tmpspace) - write_po->nboff; //arrange count of left bits in the buffer 
	write_po->flushed_bytes += complete_bytes;
}


/*
* Argument type and callback necessary for uper_encode_to_buffer().
*/
typedef struct enc_to_buf_arg 
{
	void *buffer;
	size_t left;
} enc_to_buf_arg;

//Данная функция вставляет значение разници между позицией ins_pos в битах и концом потока
//возвращает false в случае ошибки
bool insert_data(asn_per_outp_t *write_po,int32_t ins_pos)
{
	enc_to_buf_arg *mbuffer;
	asn_per_data_t read_po;
	int32_t size_of_shifted_data;
	int32_t start_byte_allign;
	int32_t start_bit_allign;
	shift_buf_t shift_buffer;
	uint8_t tail_byte;
	uint8_t tail_off;
	uint8_t in_size;
	int32_t max_size;
	int32_t extension_size;
    uint8_t r_ext;

	//инициализация свап буффера
	shift_buffer.rpos_flag=false;
	shift_buffer.wpos_flag=false;

	buf_normalize(write_po);
	//---------сохраняем хвост текущего буффера------------------
	tail_byte=write_po->buffer[0];
	tail_off=write_po->nboff;
	write_po->nbits+=write_po->nboff;
	write_po->nboff=0;
	//---------конец сохранения хвоста текущего буффера----------

	mbuffer=(enc_to_buf_arg *)write_po->op_key;
	//количество сдвигаемых данных = (выведенное кол. + биты не выведенные - позиция вставки)
	size_of_shifted_data=write_po->flushed_bytes*BYTE_SIZE-ins_pos;
	//ищем стартовую позицию у потока.
	start_byte_allign=(ins_pos/BYTE_SIZE);								//вычисляем позицию в байтах
	start_bit_allign=ins_pos-start_byte_allign*BYTE_SIZE;				//вычисляем позицию в битах
	//переводим указатель буффера на необходимую позицию
	mbuffer->buffer=(uint8_t *)mbuffer->buffer-write_po->flushed_bytes+start_byte_allign;	//вычислили указатель на первый элемент позиции
	mbuffer->left=mbuffer->left+write_po->flushed_bytes-start_byte_allign;					//исправили размер свободного места в буффере
	write_po->flushed_bytes=start_byte_allign;  //уменьшаем число байт находящихся в потоке

	//остаток перемещаем в начало буффера
	write_po->buffer[0] =((uint8_t *)mbuffer->buffer)[0];	//сдвиг на неполное количество бит необходимо учесть
	write_po->nboff = start_bit_allign;						//помещаем смещение
	//инициализация позиции чтения
	read_po.buffer=(uint8_t *)mbuffer->buffer;
	read_po.nbits=mbuffer->left*BYTE_SIZE;
	read_po.nboff=write_po->nboff;

//#ifdef DEBUG	
	printf("SIZE OF DATA 0x%x \n",size_of_shifted_data+tail_off);
//#endif //DEBUG	

	//определяем длину размера сдвигаемых данных может быть или 8 или 16 бит 
	//большие занчения пока не поддерживаются
	extension_size=((size_of_shifted_data+tail_off)/BYTE_SIZE)+1;
    r_ext=(size_of_shifted_data+tail_off)-((extension_size-1)*BYTE_SIZE);//число бит остаток масива до байта
	if(extension_size<=127)in_size=8;
	else if(size_of_shifted_data+tail_off<16384)in_size=16;
	else return false;

	write_data(&shift_buffer,&read_po,in_size); //освобождаем место под длину размера данных
	
	//помещаем размер в байтах в поток
	uper_put_length(write_po,extension_size);
	//изменил так как записываем длину в байтах а до этого была в битах
	//uper_put_length(write_po,size_of_shifted_data+tail_off);
	size_of_shifted_data-=in_size;

	max_size=MAX_SIZE*BYTE_SIZE;
	while(size_of_shifted_data>0)
	{	
		if(size_of_shifted_data<MAX_SIZE*BYTE_SIZE)
		{
			max_size=size_of_shifted_data;
		}
		size_of_shifted_data-=MAX_SIZE*BYTE_SIZE;
		write_data(&shift_buffer,&read_po,max_size);//сохраняем значения для переноса
		read_data(&shift_buffer,write_po);
	}
	read_data(&shift_buffer,write_po);
	//востанавливаем хвост
	per_put_many_bits(write_po,&(tail_byte),tail_off);
	//конец восстановления хвоста
	
	//добиваем нулями конец хвоста так как экстеншион выравнивается побайту
	tail_byte=0;
    if(r_ext!=0)
	    per_put_many_bits(write_po,&(tail_byte),BYTE_SIZE-r_ext); //добиваем нулями остаток
	
	return true;
}




