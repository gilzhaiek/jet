#include <tinyalsa/asoundlib.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "i2s_clock.h"

#include "amplifier_init.h"
#include "amplifier_log.h"

#define ID_RIFF 0x46464952
#define ID_WAVE 0x45564157
#define ID_FMT  0x20746d66
#define ID_DATA 0x61746164

struct riff_wave_header {
	uint32_t riff_id;
	uint32_t riff_sz;
	uint32_t wave_id;
};

struct chunk_header {
	uint32_t id;
	uint32_t sz;
};

struct chunk_fmt {
	uint16_t audio_format;
	uint16_t num_channels;
	uint32_t sample_rate;
	uint32_t byte_rate;
	uint16_t block_align;
	uint16_t bits_per_sample;
};

static int Close = 0;

void stream_close(int sig)
{
	/* allow the stream to be closed gracefully */
	if(sig==SIGUSR1){
		//fprintf(stdout, "signal captured!\n");
		Close = 1;
	}
}

void play_sample(FILE *file, unsigned int card, unsigned int device, unsigned int channels,
				 unsigned int rate, unsigned int bits, unsigned int period_size,
				 unsigned int period_count)
{
	struct pcm_config config;
	struct pcm *pcm;
	char *buffer;
	int size;
	int num_read;

	config.channels = channels;
	config.rate = rate;
	config.period_size = period_size;
	config.period_count = period_count;
	if (bits == 32)
		config.format = PCM_FORMAT_S32_LE;
	else if (bits == 16)
		config.format = PCM_FORMAT_S16_LE;
	config.start_threshold = 0;
	config.stop_threshold = 0;
	config.silence_threshold = 0;

	pcm = pcm_open(card, device, PCM_OUT, &config);
	if (!pcm || !pcm_is_ready(pcm)) {
		print_err("Unable to open PCM device %u (%s)\n",
				device, pcm_get_error(pcm));
		return;
	}

	size = pcm_frames_to_bytes(pcm, pcm_get_buffer_size(pcm));
	buffer = malloc(size);
	if (!buffer) {
		print_err("Unable to allocate %d bytes\n", size);
		free(buffer);
		pcm_close(pcm);
		return;
	}

	//printf("Playing sample: %u ch, %u hz, %u bit\n", channels, rate, bits);

	/* catch main thread signal to shutdown cleanly */
	if(signal(SIGUSR1, stream_close)== SIG_ERR){
		print_err("can't catch SIGUSR1\n");
		return;
	}
	do {
		num_read = fread(buffer, 1, size, file);
		if (num_read > 0) {
			if (pcm_write(pcm, buffer, num_read)) {
				print_err("Error playing sample\n");
				break;
			}
		}
	} while (!Close && num_read > 0);

	free(buffer);
	pcm_close(pcm);
}

void* i2sClockOn(void *arg)
{
	FILE *file;
	struct riff_wave_header riff_wave_header;
	struct chunk_header chunk_header;
	struct chunk_fmt chunk_fmt;
	unsigned int device = 0;
	unsigned int card = 0;
	unsigned int period_size = 1024;
	unsigned int period_count = 4;
	int more_chunks = 1;

	file = fopen(WAV_FILES, "rb");

	if (!file) {
		print_err("Unable to open file '%s'\n", WAV_FILES);
		return NULL;
	}
	
	fread(&riff_wave_header, sizeof(riff_wave_header), 1, file);
	if ((riff_wave_header.riff_id != ID_RIFF) ||
		(riff_wave_header.wave_id != ID_WAVE)) {
		print_err("Error: '%s' is not a riff/wave file\n", WAV_FILES);
		fclose(file);
		return NULL;
	}

	do {
		fread(&chunk_header, sizeof(chunk_header), 1, file);

		switch (chunk_header.id) {
		case ID_FMT:
			fread(&chunk_fmt, sizeof(chunk_fmt), 1, file);
			/* If the format header is larger, skip the rest */
			if (chunk_header.sz > sizeof(chunk_fmt))
				fseek(file, chunk_header.sz - sizeof(chunk_fmt), SEEK_CUR);
			break;
		case ID_DATA:
			/* Stop looking for chunks */
			more_chunks = 0;
			break;
		default:
			/* Unknown chunk, skip bytes */
			fseek(file, chunk_header.sz, SEEK_CUR);
		}
	} while (more_chunks);

	while (!Close)
		play_sample(file, card, device, chunk_fmt.num_channels, chunk_fmt.sample_rate,
					chunk_fmt.bits_per_sample, period_size, period_count);

	fclose(file);
	return NULL;
}