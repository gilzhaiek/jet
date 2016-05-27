#!/usr/bin/octave --persist

function plotyaw(data)
#data=csvread(filename);
#data=data(2:rows(data),:);
#data=data(:,13);
filtering_enabled = 0;
if (filtering_enabled == 1) 
  window = 50;
  x=filter(ones(window,1)/window, 1, data);
  n = window;
  y=x(n : n : end);
  data=y;
endif
plot(data,'linewidth',2)
FS = findall(gca,'-property','FontSize');
set(FS,'FontSize',18);
grid on
set(gca, 'ytick', linspace(-180,180,9))
h=get (gcf, "currentaxes");
set(h,"fontweight","bold","linewidth",2)
endfunction

function ytickunit(unit)
set(gca,'yticklabel',strcat(get(gca,'yticklabel'),unit))
endfunction

function xtickunit(unit)
set(gca,'xticklabel',strcat(get(gca,'xticklabel'),unit))
endfunction

function usage()
    printf("Usage: %s <filename> [<filename>]",program_name ());
endfunction

arg_list = argv ();
if (nargin < 1)
    usage
    exit
end

filename = arg_list{1};
if (exist(filename) != 2)
  printf ("%s cannot be found", filename);
  exit
end
data = csvread(filename);
plotyaw(data)

if (nargin == 2)
    filename2 = arg_list{2};
    if (exist(filename2) != 2)
      printf ("%s cannot be found", filename2);
      exit
    end
    data = csvread(filename2);
    hold all
    plotyaw(data)
    legend("Stock","Accel conv_B")
end
